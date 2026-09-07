package com.example.iptvplayer

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.navigation.compose.rememberNavController
import com.example.iptvplayer.navigation.AppNavGraph
import com.example.iptvplayer.ui.theme.IPTVPlayerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IPTVPlayerTheme {
                val navController = rememberNavController()
                AppNavGraph(navController = navController, viewModel = viewModel)
            }
        }
    }

    // Called when the user presses Home, switches apps, or opens Recents —
    // i.e. actually leaving the app, as opposed to pressing Back inside it.
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && viewModel.isPlayerActive.value) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        viewModel.setInPipMode(isInPictureInPictureMode)
    }

    override fun onStop() {
        super.onStop()
        if (viewModel.isPlayerActive.value) {
            viewModel.releaseActivePlayer()
        }
    }
}
