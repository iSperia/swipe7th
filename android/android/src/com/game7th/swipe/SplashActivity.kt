package com.game7th.swipe

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.installations.FirebaseInstallations

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseInstallations.getInstance().id.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                startActivity(Intent(this@SplashActivity, GdxGameActivity::class.java).apply {
                    putExtra(GdxGameActivity.ARG_INSTANCE_ID, task.result)
                })
            } else {
                Toast.makeText(this@SplashActivity, "Failed to get installation id", Toast.LENGTH_LONG)
                finish()
            }
        }
    }
}