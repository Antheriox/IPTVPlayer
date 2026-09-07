package com.example.iptvplayer.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "iptv_profiles")

class ProfileStore(private val context: Context) {
    private val gson = Gson()
    private val key = stringPreferencesKey("profiles_json")
    private val listType = object : TypeToken<MutableList<Profile>>() {}.type
    private val lastActiveKey = stringPreferencesKey("last_active_profile_name")

    val lastActiveProfileName: Flow<String?> = context.dataStore.data.map { it[lastActiveKey] }

    suspend fun setLastActiveProfile(name: String) {
        context.dataStore.edit { prefs -> prefs[lastActiveKey] = name }
    }

    val profiles: Flow<List<Profile>> = context.dataStore.data.map { prefs ->
        val json = prefs[key] ?: "[]"
        try {
            gson.fromJson<MutableList<Profile>>(json, listType) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    suspend fun saveProfile(profile: Profile) {
        context.dataStore.edit { prefs ->
            val json = prefs[key] ?: "[]"
            val list: MutableList<Profile> = gson.fromJson(json, listType) ?: mutableListOf()
            list.removeAll { it.name == profile.name }
            list.add(profile)
            prefs[key] = gson.toJson(list)
        }
    }

    suspend fun deleteProfile(name: String) {
        context.dataStore.edit { prefs ->
            val json = prefs[key] ?: "[]"
            val list: MutableList<Profile> = gson.fromJson(json, listType) ?: mutableListOf()
            list.removeAll { it.name == name }
            prefs[key] = gson.toJson(list)
        }
    }
}
