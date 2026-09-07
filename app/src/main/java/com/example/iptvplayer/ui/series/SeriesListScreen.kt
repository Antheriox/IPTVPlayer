package com.example.iptvplayer.ui.series

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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.iptvplayer.MainViewModel
import com.example.iptvplayer.data.WatchProgressStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesListScreen(
    viewModel: MainViewModel,
    categoryId: String?,
    onBack: () -> Unit,
    onOpenSeries: (Int) -> Unit,
    onResumeEpisode: (String, String, Int) -> Unit
) {
    val context = LocalContext.current
    val progressStore = remember { WatchProgressStore(context) }
    val lastWatched by progressStore.lastWatchedEpisodes.collectAsState(initial = emptyMap())

    val seriesList by viewModel.seriesList.collectAsState()

    LaunchedEffect(categoryId) {
        viewModel.loadSeriesList(categoryId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Series") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadSeriesList(categoryId) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        if (seriesList.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(seriesList) { series ->
                    val continueWatching = lastWatched[series.seriesId]

                    Card(
                        onClick = { onOpenSeries(series.seriesId) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column {
                            ListItem(
                                headlineContent = { Text(series.name) },
                                supportingContent = if (continueWatching != null) {
                                    {
                                        Text(
                                            "Continue: ${continueWatching.title} · " +
                                                "${(continueWatching.fraction * 100).toInt()}% watched"
                                        )
                                    }
                                } else null,
                                trailingContent = if (continueWatching != null) {
                                    {
                                        IconButton(onClick = {
                                            onResumeEpisode(
                                                continueWatching.streamUrl,
                                                continueWatching.title,
                                                series.seriesId
                                            )
                                        }) {
                                            Icon(Icons.Filled.PlayArrow, contentDescription = "Resume ${continueWatching.title}")
                                        }
                                    }
                                } else null,
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                            if (continueWatching != null && continueWatching.fraction > 0.02f) {
                                LinearProgressIndicator(
                                    progress = { continueWatching.fraction },
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
