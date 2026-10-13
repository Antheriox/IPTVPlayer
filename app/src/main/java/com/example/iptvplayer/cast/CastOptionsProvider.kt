package com.example.iptvplayer.cast

import android.content.Context
import com.example.iptvplayer.MainActivity
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.NotificationOptions

/**
 * Tells the Cast framework which receiver app to launch on the Chromecast.
 * Referenced from AndroidManifest.xml via OPTIONS_PROVIDER_CLASS_NAME.
 *
 * We use Google's Default Media Receiver, so no Cast Developer Console
 * registration is needed — it plays HLS, MP4 and (on newer devices) MKV.
 */
class CastOptionsProvider : OptionsProvider {

    override fun getCastOptions(context: Context): CastOptions {
        // Tapping the "now casting" notification brings the user back into the app.
        val notificationOptions = NotificationOptions.Builder()
            .setTargetActivityClassName(MainActivity::class.java.name)
            .build()
        val mediaOptions = CastMediaOptions.Builder()
            .setNotificationOptions(notificationOptions)
            .build()
        return CastOptions.Builder()
            .setReceiverApplicationId(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID)
            .setCastMediaOptions(mediaOptions)
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? = null
}
