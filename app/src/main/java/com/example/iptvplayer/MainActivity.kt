package com.example.iptvplayer

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.compose.rememberNavController
import com.example.iptvplayer.cast.CastSupport
import com.example.iptvplayer.navigation.AppNavGraph
import com.example.iptvplayer.ui.theme.IPTVPlayerTheme

// AppCompatActivity (rather than ComponentActivity) because the Cast button's
// device picker is a dialog fragment and needs a FragmentActivity host.
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialise Cast early so device discovery / session resume start right away.
        CastSupport.castContext(this)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && viewModel.isPlayerActive.value &&
            !viewModel.isCasting.value
        ) {
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
        // While casting, the TV keeps playing when the phone is locked or the
        // user switches apps, so leave the player screen alone.
        if (viewModel.isPlayerActive.value && !viewModel.isCasting.value) {
            viewModel.releaseActivePlayer()
        }
    }
}
