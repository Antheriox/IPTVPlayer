package com.example.iptvplayer.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.watchProgressDataStore by preferencesDataStore(name = "iptv_watch_progress")

data class WatchProgress(
    val streamUrl: String,
    val positionMs: Long,
    val durationMs: Long
) {
    val fraction: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

/** Which episode you were last on for a given series, so "Continue watching" can jump straight there. */
data class LastWatchedEpisode(
    val seriesId: Int,
    val streamUrl: String,
    val title: String,
    val positionMs: Long,
    val durationMs: Long,
    val watchedAt: Long
) {
    val fraction: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

/**
 * Tracks playback position for Movies & Series episodes only (Live TV has no
 * meaningful resume point), keyed by streamUrl. Also tracks, per series, which
 * episode was most recently watched.
 */
class WatchProgressStore(private val context: Context) {
    private val gson = Gson()
    private val progressKey = stringPreferencesKey("progress_json")
    private val lastEpisodeKey = stringPreferencesKey("last_watched_episode_json")
    private val progressMapType = object : TypeToken<MutableMap<String, WatchProgress>>() {}.type
    private val lastEpisodeMapType = object : TypeToken<MutableMap<String, LastWatchedEpisode>>() {}.type

    val progressMap: Flow<Map<String, WatchProgress>> = context.watchProgressDataStore.data.map { prefs ->
        val json = prefs[progressKey] ?: "{}"
        try {
            gson.fromJson<MutableMap<String, WatchProgress>>(json, progressMapType) ?: mutableMapOf()
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    // Keyed by seriesId (stored as a string internally since JSON map keys must be strings).
    val lastWatchedEpisodes: Flow<Map<Int, LastWatchedEpisode>> = context.watchProgressDataStore.data.map { prefs ->
        val json = prefs[lastEpisodeKey] ?: "{}"
        val raw: Map<String, LastWatchedEpisode> = try {
            gson.fromJson(json, lastEpisodeMapType) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
        raw.mapNotNull { (k, v) -> k.toIntOrNull()?.let { it to v } }.toMap()
    }

    suspend fun getProgress(streamUrl: String): WatchProgress? {
        val prefs = context.watchProgressDataStore.data.first()
        val json = prefs[progressKey] ?: "{}"
        val map: Map<String, WatchProgress> = try {
            gson.fromJson(json, progressMapType) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
        return map[streamUrl]
    }

    /**
     * Saves the playback position for [streamUrl]. If [seriesId] is provided
     * (i.e. this is a series episode), also records it as that series' most
     * recently watched episode.
     */
    suspend fun saveProgress(
        streamUrl: String,
        positionMs: Long,
        durationMs: Long,
        seriesId: Int? = null,
        episodeTitle: String? = null
    ) {
        if (durationMs <= 0) return
        context.watchProgressDataStore.edit { prefs ->
            val json = prefs[progressKey] ?: "{}"
            val map: MutableMap<String, WatchProgress> = try {
                gson.fromJson(json, progressMapType) ?: mutableMapOf()
            } catch (e: Exception) {
                mutableMapOf()
            }
            map[streamUrl] = WatchProgress(streamUrl, positionMs, durationMs)
            prefs[progressKey] = gson.toJson(map)

            if (seriesId != null) {
                val lastJson = prefs[lastEpisodeKey] ?: "{}"
                val lastMap: MutableMap<String, LastWatchedEpisode> = try {
                    gson.fromJson(lastJson, lastEpisodeMapType) ?: mutableMapOf()
                } catch (e: Exception) {
                    mutableMapOf()
                }
                lastMap[seriesId.toString()] = LastWatchedEpisode(
                    seriesId = seriesId,
                    streamUrl = streamUrl,
                    title = episodeTitle ?: "",
                    positionMs = positionMs,
                    durationMs = durationMs,
                    watchedAt = System.currentTimeMillis()
                )
                prefs[lastEpisodeKey] = gson.toJson(lastMap)
            }
        }
    }
}
