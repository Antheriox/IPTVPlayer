package com.example.iptvplayer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.iptvplayer.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenCategories: (String) -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val supportsMoviesAndSeries = viewModel.supportsMoviesAndSeries()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("IPTV Player") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HomeTile(
                title = "Live TV",
                subtitle = "Browse live channels",
                icon = Icons.Filled.LiveTv
            ) { onOpenCategories("LIVE") }

            if (supportsMoviesAndSeries) {
                HomeTile(
                    title = "Movies",
                    subtitle = "On-demand movies",
                    icon = Icons.Filled.Movie
                ) { onOpenCategories("MOVIES") }
                HomeTile(
                    title = "Series",
                    subtitle = "TV shows & episodes",
                    icon = Icons.Filled.VideoLibrary
                ) { onOpenCategories("SERIES") }
            } else {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        "Movies & Series need an Xtream Codes login. A plain M3U " +
                            "playlist only exposes Live TV, grouped by category.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            HomeTile(
                title = "Favorites",
                subtitle = "Your saved channels & shows",
                icon = Icons.Filled.Favorite
            ) { onOpenFavorites() }
        }
    }
}

@Composable
private fun HomeTile(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
