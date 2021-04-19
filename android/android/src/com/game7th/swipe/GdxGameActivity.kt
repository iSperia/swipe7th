package com.game7th.swipe

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import com.android.billingclient.api.*
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.game7th.metagame.shop.PurchaseItemInfo
import com.game7th.metagame.shop.PurchaseItemMapper
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

enum class BillingState {
    NOT_READY, READY
}

class GdxGameActivity : AndroidApplication() {

    lateinit var purchasesUpdatedListener: PurchasesUpdatedListener
    lateinit var billingClient: BillingClient

    var state = BillingState.NOT_READY

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

        initialize(SwipeGameGdx(storage, intent.getStringExtra(ARG_INSTANCE_ID)!!, BuildConfig.ENDPOINT, object: PurchaseItemMapper {
            override suspend fun mapItems(ids: List<String>): List<PurchaseItemInfo> {
                return if (state == BillingState.READY) {
                    val params = SkuDetailsParams.newBuilder().setSkusList(ids).setType(BillingClient.SkuType.INAPP)
                    suspendCoroutine { continuation ->
                        billingClient.querySkuDetailsAsync(params.build()) { result, details ->
                            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                                continuation.resume(details?.map {
                                    PurchaseItemInfo(it.sku, it.title, it.price, it.priceCurrencyCode)
                                } ?: emptyList())
                            } else {
                                continuation.resume(emptyList())
                            }
                        }
                    }
                } else {
                    emptyList()
                }
            }
        }) { finish() }, config)

        purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->

        }
        initBillingClient()
    }

    private fun initBillingClient() {
        billingClient = BillingClient.newBuilder(this)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases()
                .build()
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    state = BillingState.READY
                }
            }

            override fun onBillingServiceDisconnected() {
                state = BillingState.NOT_READY
            }
        })
    }

    companion object {
        const val KEY_INTRO_SHOWN = "intro.shown"
        const val ARG_INSTANCE_ID = "arg.instance_id"
    }
}
