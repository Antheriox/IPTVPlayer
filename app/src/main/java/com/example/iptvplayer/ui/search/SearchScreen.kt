package com.example.iptvplayer.ui.search

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.iptvplayer.MainViewModel
import com.example.iptvplayer.data.ContentType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    type: String,
    onBack: () -> Unit,
    onPlayChannel: (String, String) -> Unit,
    onOpenSeries: (Int) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val isSeries = type == "SERIES"
    val contentType = when (type) {
        "MOVIES" -> ContentType.MOVIES
        "SERIES" -> ContentType.SERIES
        else -> ContentType.LIVE
    }

    val channelResults by viewModel.searchChannels.collectAsState()
    val seriesResults by viewModel.searchSeries.collectAsState()
    val loading by viewModel.searchLoading.collectAsState()

    LaunchedEffect(type) {
        if (isSeries) viewModel.loadAllSeriesForSearch() else viewModel.loadAllChannelsForSearch(contentType)
    }

    val filteredChannels = remember(query, channelResults) {
        if (query.isBlank()) channelResults
        else channelResults.filter { it.name.contains(query, ignoreCase = true) }
    }
    val filteredSeries = remember(query, seriesResults) {
        if (query.isBlank()) seriesResults
        else seriesResults.filter { it.name.contains(query, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )

                if (loading && channelResults.isEmpty() && seriesResults.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (isSeries) {
                    LazyColumn(contentPadding = PaddingValues(16.dp)) {
                        items(filteredSeries) { series ->
                            Card(
                                onClick = { onOpenSeries(series.seriesId) },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                ListItem(
                                    headlineContent = { Text(series.name) },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(16.dp)) {
                        items(filteredChannels) { channel ->
                            Card(
                                onClick = { onPlayChannel(channel.streamUrl, channel.name) },
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
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
