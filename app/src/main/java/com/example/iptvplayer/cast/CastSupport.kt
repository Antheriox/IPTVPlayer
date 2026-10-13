package com.example.iptvplayer.cast

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.ContextThemeWrapper
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.DefaultMediaItemConverter
import androidx.media3.cast.MediaItemConverter
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext

/**
 * Small helper around the Google Cast SDK. Everything here must be called on
 * the main thread (the Cast SDK requires it).
 */
@OptIn(UnstableApi::class)
object CastSupport {

    private const val TAG = "CastSupport"
    private const val EXTRA_IS_LIVE = "iptv_is_live"

    private var castContext: CastContext? = null
    private var initAttempted = false
    private var castPlayer: CastPlayer? = null

    /**
     * Returns the shared CastContext, or null if casting isn't available on this
     * device (e.g. no Google Play Services, as on some Android TV boxes / Huawei).
     */
    @Suppress("DEPRECATION")
    fun castContext(context: Context): CastContext? {
        if (!initAttempted) {
            initAttempted = true
            castContext = try {
                CastContext.getSharedInstance(context.applicationContext)
            } catch (e: Exception) {
                Log.w(TAG, "Cast is not available on this device", e)
                null
            }
        }
        return castContext
    }

    /**
     * One CastPlayer for the whole app. It's deliberately never released:
     * CastPlayer.release() also disconnects from the Chromecast, and we want the
     * connection to survive moving between screens/channels.
     */
    fun castPlayer(context: Context): CastPlayer? {
        castPlayer?.let { return it }
        val ctx = castContext(context) ?: return null
        return CastPlayer(ctx, IptvMediaItemConverter()).also { castPlayer = it }
    }

    /** Friendly name of the Chromecast we're connected to, e.g. "Living Room TV". */
    fun connectedDeviceName(context: Context): String? =
        castContext(context)?.sessionManager?.currentCastSession?.castDevice?.friendlyName

    /**
     * Builds the MediaItem sent to the Chromecast. Unlike ExoPlayer, the Cast
     * receiver needs to be told the MIME type up front, and can't play raw
     * MPEG-TS live streams — so Xtream live URLs are switched to their HLS variant.
     */
    fun buildCastMediaItem(streamUrl: String, title: String, isLive: Boolean): MediaItem {
        val url = castCompatibleUrl(streamUrl, isLive)
        val extras = Bundle().apply { putBoolean(EXTRA_IS_LIVE, isLive) }
        return MediaItem.Builder()
            .setUri(url)
            .setMimeType(guessMimeType(url, isLive))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setExtras(extras)
                    .build()
            )
            .build()
    }

    // Xtream Codes panels serve every live channel as both .ts and .m3u8.
    // The Chromecast only understands the HLS (.m3u8) one.
    private val xtreamLiveTs = Regex("""(/live/[^?#]+)\.ts(?=$|[?#])""")

    private fun castCompatibleUrl(url: String, isLive: Boolean): String =
        if (isLive) url.replace(xtreamLiveTs, "$1.m3u8") else url

    private fun guessMimeType(url: String, isLive: Boolean): String {
        val path = url.substringBefore('?').substringBefore('#').lowercase()
        return when (path.substringAfterLast('.', missingDelimiterValue = "")) {
            "m3u8", "m3u" -> MimeTypes.APPLICATION_M3U8
            "mpd" -> MimeTypes.APPLICATION_MPD
            "mp4", "m4v" -> MimeTypes.VIDEO_MP4
            "mkv" -> MimeTypes.VIDEO_MATROSKA
            "webm" -> MimeTypes.VIDEO_WEBM
            "ts" -> MimeTypes.VIDEO_MP2T
            "avi" -> "video/x-msvideo"
            // Extension-less URLs: live IPTV is almost always HLS, VOD usually MP4.
            else -> if (isLive) MimeTypes.APPLICATION_M3U8 else MimeTypes.VIDEO_MP4
        }
    }

    /**
     * Wraps Media3's default converter so live channels are flagged as LIVE on
     * the receiver (no seek bar, no "ended" state), while VOD stays BUFFERED.
     */
    private class IptvMediaItemConverter : MediaItemConverter {
        private val delegate = DefaultMediaItemConverter()

        override fun toMediaQueueItem(mediaItem: MediaItem): MediaQueueItem {
            val queueItem = delegate.toMediaQueueItem(mediaItem)
            val isLive = mediaItem.mediaMetadata.extras?.getBoolean(EXTRA_IS_LIVE) == true
            val info = queueItem.media
            if (!isLive || info == null) return queueItem

            // Only copy over the fields that are set: several Builder setters
            // take non-null arguments.
            val builder = MediaInfo.Builder(info.contentId)
                .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
            info.contentUrl?.let { builder.setContentUrl(it) }
            info.contentType?.let { builder.setContentType(it) }
            info.metadata?.let { builder.setMetadata(it) }
            info.customData?.let { builder.setCustomData(it) }
            val liveInfo = builder.build()
            return MediaQueueItem.Builder(liveInfo).build()
        }

        override fun toMediaItem(mediaQueueItem: MediaQueueItem): MediaItem =
            delegate.toMediaItem(mediaQueueItem)
    }
}

/**
 * The standard Cast icon. It hides itself automatically when no Chromecast is
 * found on the network, and renders nothing if Cast isn't supported at all.
 * Tapping it opens the system device picker (or the "stop casting" dialog).
 */
@Composable
fun CastButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val castAvailable = remember { CastSupport.castContext(context) != null }
    if (!castAvailable) return

    AndroidView(
        factory = { ctx ->
            // Dark AppCompat theme -> white icon, matching the app's dark UI.
            val themed = ContextThemeWrapper(ctx, androidx.appcompat.R.style.Theme_AppCompat)
            MediaRouteButton(themed).also { button ->
                CastButtonFactory.setUpMediaRouteButton(ctx.applicationContext, button)
            }
        },
        modifier = modifier.size(48.dp)
    )
}
