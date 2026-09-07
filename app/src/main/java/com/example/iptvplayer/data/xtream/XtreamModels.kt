package com.example.iptvplayer.data.xtream

import com.google.gson.annotations.SerializedName

data class XtreamCategory(
    @SerializedName("category_id") val categoryId: String,
    @SerializedName("category_name") val categoryName: String
)

data class XtreamLiveStream(
    @SerializedName("stream_id") val streamId: Int,
    @SerializedName("name") val name: String,
    @SerializedName("stream_icon") val streamIcon: String?,
    @SerializedName("category_id") val categoryId: String?
)

data class XtreamVodStream(
    @SerializedName("stream_id") val streamId: Int,
    @SerializedName("name") val name: String,
    @SerializedName("stream_icon") val streamIcon: String?,
    @SerializedName("category_id") val categoryId: String?,
    @SerializedName("container_extension") val containerExtension: String?
)

data class XtreamSeries(
    @SerializedName("series_id") val seriesId: Int,
    @SerializedName("name") val name: String,
    @SerializedName("cover") val cover: String?,
    @SerializedName("category_id") val categoryId: String?
)

data class XtreamEpisode(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("container_extension") val containerExtension: String?
)

data class XtreamSeriesInfo(
    @SerializedName("episodes") val episodes: Map<String, List<XtreamEpisode>>?
)

data class XtreamUserInfo(
    @SerializedName("auth") val auth: Int?,
    @SerializedName("status") val status: String?,
    @SerializedName("message") val message: String?
)

data class XtreamAuthResponse(
    @SerializedName("user_info") val userInfo: XtreamUserInfo?
)
