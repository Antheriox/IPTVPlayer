package com.example.iptvplayer.ui.channel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.iptvplayer.MainViewModel
import com.example.iptvplayer.data.ContentType
import com.example.iptvplayer.data.FavoritesStore
import com.example.iptvplayer.data.WatchProgressStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelListScreen(
    viewModel: MainViewModel,
    type: String,
    categoryId: String?,
    onBack: () -> Unit,
    onPlay: (String, String) -> Unit
) {
    val context = LocalContext.current
    val favoritesStore = remember { FavoritesStore(context) }
    val progressStore = remember { WatchProgressStore(context) }
    val favorites by favoritesStore.favorites.collectAsState(initial = emptyList())
    val progressMap by progressStore.progressMap.collectAsState(initial = emptyMap())
    val scope = rememberCoroutineScope()

    val channels by viewModel.channels.collectAsState()
    val contentType = when (type) {
        "MOVIES" -> ContentType.MOVIES
        "SERIES" -> ContentType.SERIES
        else -> ContentType.LIVE
    }
    val isVod = type == "MOVIES" || type == "SERIES"

    LaunchedEffect(type, categoryId) {
        viewModel.loadChannels(contentType, categoryId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Channels") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadChannels(contentType, categoryId) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        if (channels.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(channels, key = { it.streamUrl }) { channel ->
                    val isFavorite = favorites.any { it.streamUrl == channel.streamUrl }
                    val progress = if (isVod) progressMap[channel.streamUrl] else null

                    Card(
                        onClick = { onPlay(channel.streamUrl, channel.name) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column {
                            ListItem(
                                headlineContent = { Text(channel.name) },
                                leadingContent = {
                                    AsyncImage(
                                        model = channel.logoUrl,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                },
                                trailingContent = {
                                    IconButton(onClick = {
                                        scope.launch { favoritesStore.toggleFavorite(channel) }
                                    }) {
                                        Icon(
                                            if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                        )
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                            if (progress != null && progress.fraction > 0.02f) {
                                LinearProgressIndicator(
                                    progress = { progress.fraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .padding(bottom = 10.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
