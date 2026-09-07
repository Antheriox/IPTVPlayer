package com.example.iptvplayer.data.m3u

import com.example.iptvplayer.data.Category
import com.example.iptvplayer.data.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class M3UParseException(message: String) : Exception(message)

data class M3UResult(
    val categories: List<Category>,
    val channels: List<Channel>
)

object M3UParser {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun fetchAndParse(url: String): M3UResult = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val body = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw M3UParseException("Server returned ${response.code}")
            response.body?.string() ?: throw M3UParseException("Empty playlist")
        }
        parse(body)
    }

    private fun parse(content: String): M3UResult {
        val lines = content.lines()
        val categoriesMap = LinkedHashMap<String, Category>()
        val channels = mutableListOf<Channel>()

        var currentName: String? = null
        var currentLogo: String? = null
        var currentGroup: String? = null
        var idCounter = 0

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            if (line.startsWith("#EXTINF", ignoreCase = true)) {
                currentName = line.substringAfterLast(",").trim()
                currentLogo = Regex("tvg-logo=\"([^\"]*)\"").find(line)?.groupValues?.get(1)
                currentGroup = Regex("group-title=\"([^\"]*)\"").find(line)?.groupValues?.get(1)
            } else if (!line.startsWith("#")) {
                val groupName = currentGroup ?: "Uncategorized"
                if (!categoriesMap.containsKey(groupName)) {
                    categoriesMap[groupName] = Category(groupName, groupName)
                }
                idCounter += 1
                channels.add(
                    Channel(
                        id = idCounter.toString(),
                        name = currentName ?: "Channel $idCounter",
                        logoUrl = currentLogo,
                        streamUrl = line,
                        categoryId = groupName
                    )
                )
                currentName = null
                currentLogo = null
                currentGroup = null
            }
        }

        return M3UResult(categoriesMap.values.toList(), channels)
    }
}
