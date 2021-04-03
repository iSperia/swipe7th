package com.game7th.swipe

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration

class AndroidLaunchActivity : AndroidApplication() {
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
        initialize(SwipeGameGdx(storage), config)

        if (storage.get(KEY_INTRO_SHOWN)?.toBoolean() != true) {
            startActivity(Intent(this@AndroidLaunchActivity, VideoPlaybackActivity::class.java))
        }
    }

    companion object {
        const val KEY_INTRO_SHOWN = "intro.shown"
    }
}
