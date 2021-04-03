package com.game7th.swipe

import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class VideoPlaybackActivity : AppCompatActivity() {

    lateinit var videoView: VideoView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        videoView = VideoView(this)
        setContentView(videoView)
        videoView.setMediaController(MediaController(this))
        videoView.setOnCompletionListener {
            val storage = AndroidStorage(applicationContext)
            storage.put(AndroidLaunchActivity.KEY_INTRO_SHOWN, true.toString())

            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        videoView.setVideoURI(Uri.parse("android.resource://$packageName/${R.raw.intro}"))
        videoView.start()
    }
}