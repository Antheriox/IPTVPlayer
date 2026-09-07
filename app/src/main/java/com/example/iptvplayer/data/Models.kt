package com.example.iptvplayer.data

enum class ProfileType { M3U, XTREAM }

data class Profile(
    val name: String,
    val type: ProfileType,
    val m3uUrl: String? = null,
    val serverUrl: String? = null,
    val username: String? = null,
    val password: String? = null
)

data class Category(
    val id: String,
    val name: String
)

data class Channel(
    val id: String,
    val name: String,
    val logoUrl: String?,
    val streamUrl: String,
    val categoryId: String?,
    val seriesId: Int? = null
)

sealed class ContentType {
    data object LIVE : ContentType()
    data object MOVIES : ContentType()
    data object SERIES : ContentType()
}
