package com.example.iptvplayer.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.iptvplayer.MainViewModel
import com.example.iptvplayer.data.Profile
import com.example.iptvplayer.data.ProfileStore
import com.example.iptvplayer.data.ProfileType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onProfileSwitched: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val profileStore = remember { ProfileStore(context) }
    val savedProfiles by profileStore.profiles.collectAsState(initial = emptyList())
    val loginState by viewModel.loginState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val activeProfile = viewModel.currentProfile()

    // The profile a tap on the saved-profiles list is currently switching to.
    var switchingTo by remember { mutableStateOf<Profile?>(null) }

    LaunchedEffect(loginState) {
        if (switchingTo != null) {
            when (loginState) {
                is MainViewModel.LoginState.Success -> {
                    switchingTo?.let { scope.launch { profileStore.setLastActiveProfile(it.name) } }
                    switchingTo = null
                    onProfileSwitched()
                }
                is MainViewModel.LoginState.Error -> {
                    val message = (loginState as MainViewModel.LoginState.Error).message
                    switchingTo = null
                    scope.launch { snackbarHostState.showSnackbar("Couldn't switch: $message") }
                }
                else -> Unit
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (activeProfile != null) {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Currently connected", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(activeProfile.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (activeProfile.type == ProfileType.XTREAM) "Xtream Codes" else "M3U playlist",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text("Playlist", style = MaterialTheme.typography.titleMedium)

            OutlinedButton(
                onClick = {
                    viewModel.refreshPlaylist()
                    scope.launch { snackbarHostState.showSnackbar("Playlist refreshed") }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Refresh playlist")
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            Text("Saved profiles", style = MaterialTheme.typography.titleMedium)
            Text(
                "Tap a profile to switch to it instantly.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (savedProfiles.isEmpty()) {
                Text(
                    "No saved profiles yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column {
                    savedProfiles.forEach { saved ->
                        val isSwitchingToThis = switchingTo?.name == saved.name
                        val isActive = activeProfile?.name == saved.name
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = if (isActive) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                        ) {
                            ListItem(
                                headlineContent = { Text(saved.name) },
                                supportingContent = {
                                    Text(if (saved.type == ProfileType.XTREAM) "Xtream Codes" else "M3U")
                                },
                                trailingContent = {
                                    if (isSwitchingToThis) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                    } else {
                                        IconButton(onClick = {
                                            scope.launch { profileStore.deleteProfile(saved.name) }
                                        }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Delete ${saved.name}")
                                        }
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable(enabled = switchingTo == null && !isActive) {
                                    switchingTo = saved
                                    viewModel.login(saved)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.resetLogin()
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Log out")
            }
        }
    }
}
