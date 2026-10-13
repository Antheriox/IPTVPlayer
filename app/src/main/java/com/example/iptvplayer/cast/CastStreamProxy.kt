package com.example.iptvplayer.cast

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.PowerManager
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * A tiny HTTP relay running on the phone, used when casting HLS (live TV).
 *
 * Why: the Chromecast plays HLS through JavaScript (Media Source Extensions),
 * which only works if the IPTV server sends CORS headers. Most IPTV servers
 * don't, so the receiver fails and drops back to its grey "cast" screen. MP4
 * movies aren't affected because they're played directly, without CORS.
 *
 * The relay fetches the playlist + video segments from the IPTV server and
 * serves them to the Chromecast over the local Wi-Fi with CORS headers added.
 * Playlists are rewritten so every segment / sub-playlist also goes through it.
 * Requests must carry a random per-run token, so it can't be used as an open
 * proxy by other devices on the network.
 */
object CastStreamProxy {

    private const val TAG = "CastStreamProxy"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val token = UUID.randomUUID().toString().replace("-", "")
    private val workers = Executors.newCachedThreadPool()

    @Volatile private var server: ServerSocket? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    /**
     * Returns a URL on this phone that relays [hlsUrl] for the Chromecast, or
     * null if the relay can't run (e.g. the phone isn't on Wi-Fi).
     */
    @Synchronized
    fun proxiedPlaylistUrl(context: Context, hlsUrl: String): String? {
        val ip = localIpv4Address(context) ?: return null
        val port = ensureStarted(context) ?: return null
        return "http://${ip.hostAddress}:$port/$token/p?u=${Uri.encode(hlsUrl)}"
    }

    /** Stops the relay and releases the wake/Wi-Fi locks. Safe to call any time. */
    @Synchronized
    fun stop() {
        try {
            server?.close()
        } catch (_: Exception) {
        }
        server = null
        wakeLock?.takeIf { it.isHeld }?.release()
        wifiLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        wifiLock = null
    }

    private fun ensureStarted(context: Context): Int? {
        server?.let { if (!it.isClosed) return it.localPort }
        return try {
            val socket = ServerSocket(0)
            server = socket
            acquireLocks(context.applicationContext)
            Thread({ acceptLoop(socket) }, "CastStreamProxy").apply { isDaemon = true }.start()
            socket.localPort
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't start the cast relay", e)
            null
        }
    }

    // The phone has to keep relaying with the screen off, so keep the CPU and
    // Wi-Fi awake for as long as the relay is running.
    private fun acquireLocks(context: Context) {
        val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "IPTVPlayer:cast")
            .apply { setReferenceCounted(false); acquire(6 * 60 * 60 * 1000L) }
        val wifi = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        wifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "IPTVPlayer:cast")
            .apply { setReferenceCounted(false); acquire() }
    }

    private fun acceptLoop(socket: ServerSocket) {
        while (!socket.isClosed) {
            val connection = try {
                socket.accept()
            } catch (_: Exception) {
                break
            }
            workers.execute { handle(connection) }
        }
    }

    private fun handle(socket: Socket) {
        socket.use { s ->
            try {
                s.soTimeout = 30_000
                val reader = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.ISO_8859_1))
                val requestLine = reader.readLine() ?: return
                val headers = mutableMapOf<String, String>()
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.isEmpty()) break
                    val colon = line.indexOf(':')
                    if (colon > 0) headers[line.substring(0, colon).trim().lowercase()] = line.substring(colon + 1).trim()
                }

                val parts = requestLine.split(" ")
                val method = parts.getOrNull(0) ?: return
                val target = Uri.parse("http://localhost" + (parts.getOrNull(1) ?: return))
                val out = s.getOutputStream()

                if (method == "OPTIONS") {
                    writeHead(out, 204, "No Content", emptyMap())
                    return
                }
                val segments = target.pathSegments
                val upstream = target.getQueryParameter("u")
                if (segments.size != 2 || segments[0] != token || upstream == null ||
                    !(upstream.startsWith("http://") || upstream.startsWith("https://"))
                ) {
                    writeHead(out, 404, "Not Found", mapOf("Content-Length" to "0"))
                    return
                }
                when (segments[1]) {
                    "p" -> relayPlaylist(out, upstream)
                    "s" -> relayBytes(out, upstream, headers["range"], headOnly = method == "HEAD")
                    else -> writeHead(out, 404, "Not Found", mapOf("Content-Length" to "0"))
                }
            } catch (e: Exception) {
                Log.d(TAG, "Relay request failed: ${e.message}")
            }
        }
    }

    private fun relayPlaylist(out: OutputStream, url: String) {
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) {
                writeHead(out, 502, "Bad Gateway", mapOf("Content-Length" to "0"))
                return
            }
            // Resolve relative links against the final URL (Xtream panels redirect).
            val base = response.request.url.toString()
            val body = rewritePlaylist(response.body?.string().orEmpty(), base)
            val bytes = body.toByteArray(Charsets.UTF_8)
            writeHead(
                out, 200, "OK", mapOf(
                    "Content-Type" to "application/vnd.apple.mpegurl",
                    "Content-Length" to bytes.size.toString(),
                    "Cache-Control" to "no-cache"
                )
            )
            out.write(bytes)
            out.flush()
        }
    }

    private fun relayBytes(out: OutputStream, url: String, range: String?, headOnly: Boolean) {
        val request = Request.Builder().url(url)
            .apply { if (range != null) header("Range", range) }
            .build()
        client.newCall(request).execute().use { response: Response ->
            val headers = linkedMapOf<String, String>()
            response.header("Content-Type")?.let { headers["Content-Type"] = it }
            response.header("Content-Length")?.let { headers["Content-Length"] = it }
            response.header("Content-Range")?.let { headers["Content-Range"] = it }
            response.header("Accept-Ranges")?.let { headers["Accept-Ranges"] = it }
            writeHead(out, response.code, response.message.ifEmpty { "OK" }, headers)
            if (!headOnly) response.body?.byteStream()?.copyTo(out, 64 * 1024)
            out.flush()
        }
    }

    private fun rewritePlaylist(playlist: String, base: String): String {
        val uriAttr = Regex("""URI="([^"]+)"""")
        var nextIsPlaylist = false
        return playlist.lineSequence().joinToString("\n") { raw ->
            val line = raw.trim()
            when {
                line.isEmpty() -> line
                line.startsWith("#") -> {
                    if (line.startsWith("#EXT-X-STREAM-INF")) nextIsPlaylist = true
                    // Audio/subtitle renditions are playlists; keys and init segments are bytes.
                    val attrIsPlaylist = line.startsWith("#EXT-X-MEDIA") ||
                        line.startsWith("#EXT-X-I-FRAME-STREAM-INF")
                    uriAttr.replace(line) { m ->
                        "URI=\"${proxied(resolve(base, m.groupValues[1]), attrIsPlaylist)}\""
                    }
                }
                else -> {
                    val absolute = resolve(base, line)
                    val isPlaylist = nextIsPlaylist ||
                        absolute.substringBefore('?').lowercase().let { it.endsWith(".m3u8") || it.endsWith(".m3u") }
                    nextIsPlaylist = false
                    proxied(absolute, isPlaylist)
                }
            }
        }
    }

    private fun proxied(absoluteUrl: String, isPlaylist: Boolean): String =
        "/$token/${if (isPlaylist) "p" else "s"}?u=${Uri.encode(absoluteUrl)}"

    private fun resolve(base: String, ref: String): String =
        try {
            URI(base).resolve(ref.replace(" ", "%20")).toString()
        } catch (_: Exception) {
            ref
        }

    private fun writeHead(out: OutputStream, code: Int, message: String, headers: Map<String, String>) {
        val sb = StringBuilder("HTTP/1.1 $code $message\r\n")
        sb.append("Access-Control-Allow-Origin: *\r\n")
        sb.append("Access-Control-Allow-Methods: GET, HEAD, OPTIONS\r\n")
        sb.append("Access-Control-Allow-Headers: *\r\n")
        sb.append("Access-Control-Expose-Headers: Content-Length, Content-Range\r\n")
        sb.append("Connection: close\r\n")
        headers.forEach { (k, v) -> sb.append("$k: $v\r\n") }
        sb.append("\r\n")
        out.write(sb.toString().toByteArray(Charsets.ISO_8859_1))
    }

    /** The phone's IPv4 address on the local network (where the Chromecast lives). */
    private fun localIpv4Address(context: Context): InetAddress? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.getLinkProperties(cm.activeNetwork)?.linkAddresses
            ?.map { it.address }
            ?.firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
            ?.let { return it }
        // Fallback: any site-local IPv4 address (e.g. 192.168.x.x).
        return try {
            NetworkInterface.getNetworkInterfaces()?.toList()
                ?.flatMap { it.inetAddresses.toList() }
                ?.firstOrNull { it is Inet4Address && it.isSiteLocalAddress }
        } catch (_: Exception) {
            null
        }
    }
}
