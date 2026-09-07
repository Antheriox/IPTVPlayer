package com.example.iptvplayer.ui.favorites

import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.iptvplayer.data.Channel
import com.example.iptvplayer.data.FavoritesStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(onBack: () -> Unit, onPlay: (Channel) -> Unit) {
    val context = LocalContext.current
    val favoritesStore = remember { FavoritesStore(context) }
    val favorites by favoritesStore.favorites.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorites") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (favorites.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "No favorites yet. Tap the heart icon on a channel, movie, or episode to add it here.",
                    modifier = Modifier.padding(32.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(favorites, key = { it.streamUrl }) { channel ->
                    Card(
                        onClick = { onPlay(channel) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
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
                                        Icons.Filled.Favorite,
                                        contentDescription = "Remove from favorites",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}
