package com.game7th.swipe

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth


class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseAnalytics.getInstance(applicationContext)

        // Configure Google Sign In
        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                .requestIdToken("668899100685-u68s72n9ugh2rg83ea2alidemqui45us.apps.googleusercontent.com")
                .requestEmail()
                .build()

        GoogleSignIn.getClient(this, gso).signInIntent.let { startActivityForResult(it, RC_SIGN_IN) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account: GoogleSignInAccount? = task.getResult(ApiException::class.java)
                account?.id?.let { accountId ->
                    finish()
                    startActivity(Intent(this@SplashActivity, GdxGameActivity::class.java).apply {
                        putExtra(GdxGameActivity.ARG_INSTANCE_ID, accountId)
                        addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                        overridePendingTransition(0, 0)
                    })
                }
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e)
            }
        }
    }

    companion object {
        const val TAG = "SWIPE"
        const val RC_SIGN_IN = 10
    }
}