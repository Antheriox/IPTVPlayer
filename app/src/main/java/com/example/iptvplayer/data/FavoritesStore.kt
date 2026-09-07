package com.example.iptvplayer.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.favoritesDataStore by preferencesDataStore(name = "iptv_favorites")

/**
 * Favorites are keyed by streamUrl (unique per playable item), so a live
 * channel, a movie, and a series episode can all be favorited the same way.
 */
class FavoritesStore(private val context: Context) {
    private val gson = Gson()
    private val key = stringPreferencesKey("favorites_json")
    private val listType = object : TypeToken<MutableList<Channel>>() {}.type

    val favorites: Flow<List<Channel>> = context.favoritesDataStore.data.map { prefs ->
        val json = prefs[key] ?: "[]"
        try {
            gson.fromJson<MutableList<Channel>>(json, listType) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    suspend fun toggleFavorite(channel: Channel) {
        context.favoritesDataStore.edit { prefs ->
            val json = prefs[key] ?: "[]"
            val list: MutableList<Channel> = try {
                gson.fromJson(json, listType) ?: mutableListOf()
            } catch (e: Exception) {
                mutableListOf()
            }
            val existingIndex = list.indexOfFirst { it.streamUrl == channel.streamUrl }
            if (existingIndex >= 0) {
                list.removeAt(existingIndex)
            } else {
                list.add(channel)
            }
            prefs[key] = gson.toJson(list)
        }
    }
}
