package com.example.iptvplayer.ui.episode

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.iptvplayer.MainViewModel
import com.example.iptvplayer.data.FavoritesStore
import com.example.iptvplayer.data.WatchProgressStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeListScreen(
    viewModel: MainViewModel,
    seriesId: Int,
    onBack: () -> Unit,
    onPlay: (String, String) -> Unit
) {
    val context = LocalContext.current
    val favoritesStore = remember { FavoritesStore(context) }
    val progressStore = remember { WatchProgressStore(context) }
    val favorites by favoritesStore.favorites.collectAsState(initial = emptyList())
    val progressMap by progressStore.progressMap.collectAsState(initial = emptyMap())
    val scope = rememberCoroutineScope()

    val episodes by viewModel.episodes.collectAsState()

    LaunchedEffect(seriesId) {
        viewModel.loadEpisodes(seriesId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Episodes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadEpisodes(seriesId) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        if (episodes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(episodes, key = { it.streamUrl }) { ep ->
                    val isFavorite = favorites.any { it.streamUrl == ep.streamUrl }
                    val progress = progressMap[ep.streamUrl]

                    Card(
                        onClick = { onPlay(ep.streamUrl, ep.name) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column {
                            ListItem(
                                headlineContent = { Text(ep.name) },
                                trailingContent = {
                                    IconButton(onClick = {
                                        scope.launch { favoritesStore.toggleFavorite(ep) }
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
