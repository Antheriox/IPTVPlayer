package com.example.iptvplayer.data

import com.example.iptvplayer.data.m3u.M3UParser
import com.example.iptvplayer.data.m3u.M3UResult
import com.example.iptvplayer.data.xtream.XtreamRepository
import com.example.iptvplayer.data.xtream.XtreamSeries

/**
 * Wraps either an Xtream Codes login or a plain M3U playlist behind one
 * common interface, so the UI layer doesn't need to know which kind of
 * profile is active.
 */
class PlaylistRepository(private val profile: Profile) {

    private val xtream: XtreamRepository? = if (profile.type == ProfileType.XTREAM) {
        val server = profile.serverUrl?.trim()?.trimEnd('/')
        val user = profile.username?.trim()
        val pass = profile.password
        if (server.isNullOrBlank() || user.isNullOrBlank() || pass.isNullOrBlank()) {
            throw IllegalArgumentException("Please fill in server, username and password")
        }
        XtreamRepository(serverUrl = server, username = user, password = pass)
    } else null

    private var m3uCache: M3UResult? = null

    private suspend fun loadM3U(): M3UResult {
        val url = profile.m3uUrl?.trim()
        if (url.isNullOrBlank()) throw IllegalArgumentException("Please enter an M3U playlist URL")
        return m3uCache ?: M3UParser.fetchAndParse(url).also { m3uCache = it }
    }

    fun supportsMoviesAndSeries(): Boolean = profile.type == ProfileType.XTREAM

    fun currentProfile(): Profile = profile

    fun clearCache() {
        m3uCache = null
    }

    suspend fun validateLogin(): Boolean {
        return xtream?.login() ?: run {
            loadM3U()
            true
        }
    }

    suspend fun getCategories(type: ContentType): List<Category> {
        if (profile.type == ProfileType.M3U) {
            return if (type is ContentType.LIVE) loadM3U().categories else emptyList()
        }
        return when (type) {
            is ContentType.LIVE -> xtream!!.getLiveCategories()
            is ContentType.MOVIES -> xtream!!.getVodCategories()
            is ContentType.SERIES -> xtream!!.getSeriesCategories()
        }
    }

    suspend fun getChannels(type: ContentType, categoryId: String?): List<Channel> {
        if (profile.type == ProfileType.M3U) {
            val all = loadM3U().channels
            return if (categoryId == null) all else all.filter { it.categoryId == categoryId }
        }
        return when (type) {
            is ContentType.LIVE -> xtream!!.getLiveStreams(categoryId)
            is ContentType.MOVIES -> xtream!!.getVodStreams(categoryId)
            is ContentType.SERIES -> emptyList()
        }
    }

    suspend fun getSeriesList(categoryId: String?): List<XtreamSeries> =
        xtream?.getSeries(categoryId) ?: emptyList()

    suspend fun getSeriesEpisodes(seriesId: Int): List<Channel> =
        xtream?.getSeriesEpisodes(seriesId) ?: emptyList()
}
