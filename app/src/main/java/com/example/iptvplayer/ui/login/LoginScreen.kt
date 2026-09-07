package com.example.iptvplayer.ui.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.iptvplayer.MainViewModel
import com.example.iptvplayer.data.Profile
import com.example.iptvplayer.data.ProfileStore
import com.example.iptvplayer.data.ProfileType

@Composable
fun LoginScreen(viewModel: MainViewModel, onLoggedIn: () -> Unit) {
    val context = LocalContext.current
    val profileStore = remember { ProfileStore(context) }

    val savedProfiles by profileStore.profiles.collectAsState(initial = emptyList())
    val loginState by viewModel.loginState.collectAsState()
    val lastActiveName by profileStore.lastActiveProfileName.collectAsState(initial = null)

    var mode by remember { mutableStateOf(ProfileType.XTREAM) }
    var profileName by remember { mutableStateOf("") }
    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var m3uUrl by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var autoLoginAttempted by remember { mutableStateOf(false) }

    // Tracks whichever profile the *current* login attempt is for, so the
    // success handler saves the right one — not whatever happens to be typed
    // into the form fields at that moment.
    var pendingProfile by remember { mutableStateOf<Profile?>(null) }

    fun buildProfile() = Profile(
        name = profileName.ifBlank { "My Playlist" },
        type = mode,
        m3uUrl = m3uUrl.ifBlank { null },
        serverUrl = serverUrl.trim().trimEnd('/').ifBlank { null },
        username = username.ifBlank { null },
        password = password.ifBlank { null }
    )

    LaunchedEffect(loginState) {
        if (loginState is MainViewModel.LoginState.Success) {
            pendingProfile?.let {
                profileStore.saveProfile(it)
                profileStore.setLastActiveProfile(it.name)
            }
            onLoggedIn()
        }
    }

    LaunchedEffect(savedProfiles, lastActiveName) {
        if (!autoLoginAttempted && lastActiveName != null && savedProfiles.isNotEmpty() &&
            loginState is MainViewModel.LoginState.Idle
        ) {
            autoLoginAttempted = true
            val match = savedProfiles.firstOrNull { it.name == lastActiveName }
            if (match != null) {
                pendingProfile = match
                viewModel.login(match)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Text("IPTV Player", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        TabRow(selectedTabIndex = if (mode == ProfileType.XTREAM) 0 else 1) {
            Tab(
                selected = mode == ProfileType.XTREAM,
                onClick = { mode = ProfileType.XTREAM },
                text = { Text("Xtream Codes") }
            )
            Tab(
                selected = mode == ProfileType.M3U,
                onClick = { mode = ProfileType.M3U },
                text = { Text("M3U URL") }
            )
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = profileName,
            onValueChange = { profileName = it },
            label = { Text("Profile name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        if (mode == ProfileType.XTREAM) {
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("Server URL (http://host:port)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            OutlinedTextField(
                value = m3uUrl,
                onValueChange = { m3uUrl = it },
                label = { Text("M3U playlist URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(16.dp))

        if (validationError != null) {
            Text(validationError!!, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }

        if (loginState is MainViewModel.LoginState.Error) {
            Text(
                (loginState as MainViewModel.LoginState.Error).message,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = {
                validationError = when {
                    mode == ProfileType.XTREAM && serverUrl.isBlank() -> "Please enter the server URL"
                    mode == ProfileType.XTREAM && username.isBlank() -> "Please enter a username"
                    mode == ProfileType.XTREAM && password.isBlank() -> "Please enter a password"
                    mode == ProfileType.M3U && m3uUrl.isBlank() -> "Please enter the M3U playlist URL"
                    else -> null
                }
                if (validationError == null) {
                    val profile = buildProfile()
                    pendingProfile = profile
                    viewModel.login(profile)
                }
            },
            enabled = loginState !is MainViewModel.LoginState.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loginState is MainViewModel.LoginState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text("Connect")
            }
        }

        if (savedProfiles.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text("Saved playlists", style = MaterialTheme.typography.titleMedium)
            Column(modifier = Modifier.fillMaxWidth()) {
                savedProfiles.forEach { saved ->
                    val isConnectingToThis = loginState is MainViewModel.LoginState.Loading &&
                        pendingProfile?.name == saved.name
                    ListItem(
                        headlineContent = { Text(saved.name) },
                        supportingContent = {
                            Text(if (saved.type == ProfileType.XTREAM) "Xtream Codes" else "M3U")
                        },
                        trailingContent = {
                            if (isConnectingToThis) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            }
                        },
                        modifier = Modifier.clickable(enabled = loginState !is MainViewModel.LoginState.Loading) {
                            pendingProfile = saved
                            viewModel.login(saved)
                        }
                    )
                }
            }
        }
    }
}
