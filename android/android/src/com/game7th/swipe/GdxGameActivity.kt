package com.game7th.swipe

import android.os.Bundle
import android.view.View
import com.android.billingclient.api.*
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.game7th.metagame.CloudEnvironment
import com.game7th.metagame.shop.PurchaseItemInfo
import com.game7th.metagame.shop.PurchaseItemMapper
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

enum class BillingState {
    NOT_READY, READY
}

class GdxGameActivity : AndroidApplication() {

    lateinit var game: SwipeGameGdx
    lateinit var purchasesUpdatedListener: PurchasesUpdatedListener
    lateinit var billingClient: BillingClient
    val skuDetails = mutableListOf<SkuDetails>()

    var purchaseHandler: ((String) -> Unit)? = null

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

        game = SwipeGameGdx(storage, intent.getStringExtra(ARG_INSTANCE_ID)!!,
                CloudEnvironment(BuildConfig.ENDPOINT), object : PurchaseItemMapper {
            override suspend fun purchase(id: String): String {
                return skuDetails.firstOrNull { it.sku == id }?.let { sku ->
                    val params = BillingFlowParams.newBuilder()
                            .setSkuDetails(sku)
                            .build()
                    billingClient.launchBillingFlow(this@GdxGameActivity, params)
                    suspendCoroutine<String> { continuation ->
                        purchaseHandler = { token ->
                            continuation.resume(token)
                        }
                    }
                } ?: ""
            }

            override suspend fun consume(purchaseToken: String) {
                val consumeParams =
                        ConsumeParams.newBuilder()
                                .setPurchaseToken(purchaseToken)
                                .build()
                billingClient.consumeAsync(consumeParams) { billingResult, outToken ->
                    //TODO: do something
                }
            }

            override suspend fun mapItems(ids: List<String>): List<PurchaseItemInfo> {
                return if (state == BillingState.READY) {
                    val params = SkuDetailsParams.newBuilder().setSkusList(ids).setType(BillingClient.SkuType.INAPP)
                    suspendCoroutine { continuation ->
                        billingClient.querySkuDetailsAsync(params.build()) { result, details ->
                            details?.let { skuDetails.addAll(it) }
                            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                                continuation.resume(details?.map {
                                    PurchaseItemInfo(it.sku, it.title, it.price, it.priceCurrencyCode)
                                }?.sortedBy { ids.indexOf(it.id) } ?: emptyList())
                            } else {
                                continuation.resume(emptyList())
                            }
                        }
                    }
                } else {
                    emptyList()
                }
            }
        }) { finish() }
        initialize(game, config)

        purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
            }
        }
        initBillingClient()
    }

    private fun handlePurchase(purchase: Purchase) {
        purchaseHandler?.invoke(purchase.purchaseToken)
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

                    val purchases = billingClient.queryPurchases("inapp")
                    purchases.purchasesList?.forEach { purchase ->
                        game.restorePurchase(purchase.sku, purchase.purchaseToken)
                    }
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
        const val ARG_EMAIL = "arg.email"
    }
}
