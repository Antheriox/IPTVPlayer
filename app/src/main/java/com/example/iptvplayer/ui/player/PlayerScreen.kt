package com.example.iptvplayer.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.iptvplayer.MainViewModel
import com.example.iptvplayer.data.WatchProgressStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.view.View
import androidx.annotation.OptIn
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.media3.common.util.UnstableApi

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    viewModel: MainViewModel,
    streamUrl: String,
    title: String,
    seriesId: Int?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val progressStore = remember { WatchProgressStore(context) }
    val isInPip by viewModel.isInPipMode.collectAsState()

    var errorMessage by remember { mutableStateOf<String?>(null) }

    var controlsVisible by remember { mutableStateOf(true) }

    // Movies/series episodes get resume + progress tracking; live streams don't
    // have a meaningful "resume point" so they're left alone.
    val isVod = remember(streamUrl) {
        streamUrl.contains("/movie/") || streamUrl.contains("/series/")
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(streamUrl))
            prepare()
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    errorMessage = "Couldn't play this stream (${error.errorCodeName}). " +
                        "The server may not support this format, or the link may be invalid."
                }
            })
        }
    }

    // Tells MainActivity a video is on screen, so it knows to enter
    // Picture-in-Picture if the user leaves the app (Home/Recents/app switch).
    DisposableEffect(Unit) {
        viewModel.setPlayerActive(true)
        viewModel.registerPlayerReleaseCallback { onBack() }
        onDispose {
            viewModel.setPlayerActive(false)
            viewModel.registerPlayerReleaseCallback(null)
        }
    }

    // Force landscape for the duration of playback, restoring normal rotation
    // behavior on the way out. Safe against Activity recreation because the
    // manifest already declares orientation in configChanges (for PiP).
    DisposableEffect(Unit) {
        val previousOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation =
                previousOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    LaunchedEffect(streamUrl) {
        if (isVod) {
            val saved = progressStore.getProgress(streamUrl)
            if (saved != null && saved.durationMs > 0 &&
                saved.positionMs > 5_000 && saved.positionMs < saved.durationMs - 5_000
            ) {
                exoPlayer.seekTo(saved.positionMs)
            }
        }
    }

    LaunchedEffect(streamUrl) {
        if (isVod) {
            while (true) {
                delay(5_000)
                val duration = exoPlayer.duration
                val position = exoPlayer.currentPosition
                if (duration > 0) {
                    progressStore.saveProgress(
                        streamUrl = streamUrl,
                        positionMs = position,
                        durationMs = duration,
                        seriesId = seriesId,
                        episodeTitle = title
                    )
                }
            }
        }
    }

    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (isVod) {
                val duration = exoPlayer.duration
                val position = exoPlayer.currentPosition
                if (duration > 0) {
                    scope.launch {
                        progressStore.saveProgress(
                            streamUrl = streamUrl,
                            positionMs = position,
                            durationMs = duration,
                            seriesId = seriesId,
                            episodeTitle = title
                        )
                    }
                }
            }
            exoPlayer.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = true
                    setShowSubtitleButton(true)
                    setControllerVisibilityListener(
                        PlayerView.ControllerVisibilityListener { visibility ->
                            controlsVisible = visibility == View.VISIBLE
                        }
                    )
                }
            },
            update = { playerView ->
                // No room (or need) for scrubbing controls in the tiny PiP window.
                playerView.useController = !isInPip
            },
            modifier = Modifier.fillMaxSize()
        )

        if (errorMessage != null && !isInPip && controlsVisible) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.align(Alignment.Center).padding(24.dp)
            ) {
                Text(
                    errorMessage!!,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(20.dp)
                )
            }
        }

        if (!isInPip && controlsVisible) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
            ) {
                Surface(color = Color.Black.copy(alpha = 0.5f), shape = CircleShape) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                Text(title, color = Color.White, modifier = Modifier.padding(8.dp))
            }
        }
    }
}
