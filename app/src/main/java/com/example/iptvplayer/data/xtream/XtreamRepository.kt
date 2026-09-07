package com.example.iptvplayer.data.xtream

import com.example.iptvplayer.data.Category
import com.example.iptvplayer.data.Channel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class XtreamException(message: String) : Exception(message)

/**
 * Talks to the Xtream Codes "player_api.php" endpoint that most IPTV
 * subscription providers expose. serverUrl must NOT have a trailing slash,
 * e.g. "http://example.com:8080".
 */
class XtreamRepository(
    private val serverUrl: String,
    private val username: String,
    private val password: String
) {
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun apiUrl(action: String, extra: String = ""): String =
        "$serverUrl/player_api.php?username=$username&password=$password&action=$action$extra"

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw XtreamException("Server returned ${response.code}")
            response.body?.string() ?: throw XtreamException("Empty response")
        }
    }

    suspend fun login(): Boolean {
        val json = get("$serverUrl/player_api.php?username=$username&password=$password")
        val auth = gson.fromJson(json, XtreamAuthResponse::class.java)
        return auth?.userInfo?.auth == 1
    }

    suspend fun getLiveCategories(): List<Category> {
        val json = get(apiUrl("get_live_categories"))
        val type = object : TypeToken<List<XtreamCategory>>() {}.type
        val list: List<XtreamCategory> = gson.fromJson(json, type) ?: emptyList()
        return list.map { Category(it.categoryId, it.categoryName) }
    }

    suspend fun getLiveStreams(categoryId: String?): List<Channel> {
        val extra = if (categoryId != null) "&category_id=$categoryId" else ""
        val json = get(apiUrl("get_live_streams", extra))
        val type = object : TypeToken<List<XtreamLiveStream>>() {}.type
        val list: List<XtreamLiveStream> = gson.fromJson(json, type) ?: emptyList()
        return list.map {
            Channel(
                id = it.streamId.toString(),
                name = it.name,
                logoUrl = it.streamIcon,
                streamUrl = "$serverUrl/live/$username/$password/${it.streamId}.ts",
                categoryId = it.categoryId
            )
        }
    }

    suspend fun getVodCategories(): List<Category> {
        val json = get(apiUrl("get_vod_categories"))
        val type = object : TypeToken<List<XtreamCategory>>() {}.type
        val list: List<XtreamCategory> = gson.fromJson(json, type) ?: emptyList()
        return list.map { Category(it.categoryId, it.categoryName) }
    }

    suspend fun getVodStreams(categoryId: String?): List<Channel> {
        val extra = if (categoryId != null) "&category_id=$categoryId" else ""
        val json = get(apiUrl("get_vod_streams", extra))
        val type = object : TypeToken<List<XtreamVodStream>>() {}.type
        val list: List<XtreamVodStream> = gson.fromJson(json, type) ?: emptyList()
        return list.map {
            val ext = it.containerExtension ?: "mp4"
            Channel(
                id = it.streamId.toString(),
                name = it.name,
                logoUrl = it.streamIcon,
                streamUrl = "$serverUrl/movie/$username/$password/${it.streamId}.$ext",
                categoryId = it.categoryId
            )
        }
    }

    suspend fun getSeriesCategories(): List<Category> {
        val json = get(apiUrl("get_series_categories"))
        val type = object : TypeToken<List<XtreamCategory>>() {}.type
        val list: List<XtreamCategory> = gson.fromJson(json, type) ?: emptyList()
        return list.map { Category(it.categoryId, it.categoryName) }
    }

    suspend fun getSeries(categoryId: String?): List<XtreamSeries> {
        val extra = if (categoryId != null) "&category_id=$categoryId" else ""
        val json = get(apiUrl("get_series", extra))
        val type = object : TypeToken<List<XtreamSeries>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    suspend fun getSeriesEpisodes(seriesId: Int): List<Channel> {
        val json = get(apiUrl("get_series_info", "&series_id=$seriesId"))
        val info = gson.fromJson(json, XtreamSeriesInfo::class.java)
        val result = mutableListOf<Channel>()
        info?.episodes?.toSortedMap(compareBy { it.toIntOrNull() ?: 0 })?.forEach { (season, eps) ->
            eps.forEach { ep ->
                val ext = ep.containerExtension ?: "mp4"
                result.add(
                    Channel(
                        id = ep.id,
                        name = "S$season · ${ep.title}",
                        logoUrl = null,
                        streamUrl = "$serverUrl/series/$username/$password/${ep.id}.$ext",
                        categoryId = null,
                        seriesId = seriesId
                    )
                )
            }
        }
        return result
    }
}
