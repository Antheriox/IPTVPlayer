package com.example.iptvplayer.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
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
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.cast.SessionAvailabilityListener
import com.example.iptvplayer.MainViewModel
import com.example.iptvplayer.cast.CastButton
import com.example.iptvplayer.cast.CastStreamProxy
import com.example.iptvplayer.cast.CastSupport
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.media.RemoteMediaClient
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

    var controlsVisible by remember { mutableStateOf(false) }

    // Movies/series episodes get resume + progress tracking; live streams don't
    // have a meaningful "resume point" so they're left alone.
    val isVod = remember(streamUrl) {
        streamUrl.contains("/movie/") || streamUrl.contains("/series/")
    }

    // Shared, app-wide CastPlayer (null when the device has no Cast support).
    val castPlayer = remember { CastSupport.castPlayer(context) }
    var castDeviceName by remember { mutableStateOf<String?>(null) }

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

    // Whichever player is currently in charge: the local ExoPlayer, or the
    // CastPlayer while a Chromecast session is active.
    var currentPlayer by remember { mutableStateOf<Player>(exoPlayer) }

    // CastPlayer doesn't report receiver-side load failures, so watch the
    // receiver's status directly: otherwise a stream the TV can't play just
    // leaves the phone saying "Casting" over a grey TV screen.
    val receiverWatcher = remember {
        object : RemoteMediaClient.Callback() {
            var client: RemoteMediaClient? = null

            override fun onStatusUpdated() {
                val status = client?.mediaStatus ?: return
                if (status.playerState == MediaStatus.PLAYER_STATE_IDLE &&
                    status.idleReason == MediaStatus.IDLE_REASON_ERROR
                ) {
                    errorMessage = "The Chromecast couldn't play this stream. The IPTV server " +
                        "may block casting, or the video/audio format isn't supported by Chromecast."
                }
            }

            fun attach(newClient: RemoteMediaClient?) {
                detach()
                client = newClient
                newClient?.registerCallback(this)
            }

            fun detach() {
                client?.unregisterCallback(this)
                client = null
            }
        }
    }

    // Hand playback over to the Chromecast, continuing where the phone was.
    fun playOnCast() {
        val cast = castPlayer ?: return
        if (currentPlayer === cast) return
        val position = exoPlayer.currentPosition
        exoPlayer.stop()
        val castMediaItem = CastSupport.buildCastMediaItem(context, streamUrl, title, isLive = !isVod)
        if (isVod) cast.setMediaItem(castMediaItem, position)
        else cast.setMediaItem(castMediaItem)
        cast.playWhenReady = true
        cast.prepare()
        currentPlayer = cast
        errorMessage = null
        receiverWatcher.attach(CastSupport.remoteMediaClient(context))
        castDeviceName = CastSupport.connectedDeviceName(context)
        viewModel.setCasting(true)
    }

    // Cast session ended (user tapped "stop casting", or the TV went away):
    // pick up again on the phone at the position the TV had reached.
    fun playOnPhone() {
        val cast = castPlayer
        if (cast == null || currentPlayer !== cast) return
        val position = if (isVod) cast.currentPosition else C.TIME_UNSET
        currentPlayer = exoPlayer
        castDeviceName = null
        errorMessage = null
        receiverWatcher.detach()
        CastStreamProxy.stop()
        viewModel.setCasting(false)
        if (position != C.TIME_UNSET && position > 0) exoPlayer.seekTo(position)
        exoPlayer.playWhenReady = true
        exoPlayer.prepare()
    }

    val castErrorListener = remember {
        object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                errorMessage = "The Chromecast couldn't play this stream (${error.errorCodeName}). " +
                    "Some IPTV servers or formats (e.g. MKV, raw .ts) aren't supported by Chromecast."
            }
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
        // Already connected to a Chromecast (e.g. via the Cast button on the
        // home screen)? Then send this channel/movie straight to the TV. Done
        // after the resume seek so the TV starts at the saved position too.
        if (castPlayer?.isCastSessionAvailable == true) playOnCast()
    }

    DisposableEffect(castPlayer) {
        val cast = castPlayer
        cast?.addListener(castErrorListener)
        cast?.setSessionAvailabilityListener(object : SessionAvailabilityListener {
            override fun onCastSessionAvailable() = playOnCast()
            override fun onCastSessionUnavailable() = playOnPhone()
        })
        onDispose {
            if (cast != null) {
                cast.setSessionAvailabilityListener(null)
                cast.removeListener(castErrorListener)
                // Leaving the player = stop watching, on the TV too. The Cast
                // connection itself stays up, so the next channel casts directly.
                if (currentPlayer === cast) {
                    cast.stop()
                    cast.clearMediaItems()
                }
            }
            receiverWatcher.detach()
            CastStreamProxy.stop()
            viewModel.setCasting(false)
        }
    }

    LaunchedEffect(streamUrl) {
        if (isVod) {
            while (true) {
                delay(5_000)
                val duration = currentPlayer.duration
                val position = currentPlayer.currentPosition
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
                val duration = currentPlayer.duration
                val position = currentPlayer.currentPosition
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
                val casting = currentPlayer !== exoPlayer
                if (playerView.player !== currentPlayer) playerView.player = currentPlayer
                // No room (or need) for scrubbing controls in the tiny PiP window.
                playerView.useController = !isInPip
                // While casting the phone is just a remote: keep the controls up.
                playerView.controllerShowTimeoutMs = if (casting) 0 else 5_000
                playerView.controllerHideOnTouch = !casting
                if (casting) playerView.showController()
            },
            modifier = Modifier.fillMaxSize()
        )

        if (currentPlayer !== exoPlayer && errorMessage == null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.6f), MaterialTheme.shapes.medium)
                    .padding(20.dp)
            ) {
                Icon(
                    Icons.Filled.Cast,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    castDeviceName?.let { "Casting to $it" } ?: "Casting to Chromecast",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        if (errorMessage != null && !isInPip && (controlsVisible || currentPlayer !== exoPlayer)) {
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                Surface(color = Color.Black.copy(alpha = 0.5f)) {
                    Text(title, color = Color.White, modifier = Modifier.padding(8.dp))
                }
                // Hides itself when no Chromecast is on the network.
                CastButton()
            }
        }
    }
}
