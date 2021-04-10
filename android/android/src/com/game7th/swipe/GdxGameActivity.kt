package com.game7th.swipe

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration

class GdxGameActivity : AndroidApplication() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.apply {
            // Hide both the navigation bar and the status bar.
            // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
            // a general rule, you should design your app to hide the status bar whenever you
            // hide the navigation bar.
            systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
        val config = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
            numSamples = 2
        }

        val storage = AndroidStorage(applicationContext)

        setLogLevel(LOG_NONE)

        initialize(SwipeGameGdx(storage, intent.getStringExtra(ARG_INSTANCE_ID)!!, BuildConfig.ENDPOINT) { finish() }, config)
    }

    companion object {
        const val KEY_INTRO_SHOWN = "intro.shown"
        const val ARG_INSTANCE_ID = "arg.instance_id"
    }
}
