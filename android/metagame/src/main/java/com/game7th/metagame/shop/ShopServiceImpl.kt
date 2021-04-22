package com.game7th.metagame.shop

import com.game7th.metagame.account.AccountService
import com.game7th.metagame.network.CloudApi
import com.game7th.metagame.network.NetworkError
import com.game7th.metagame.network.NetworkErrorStatus
import com.game7th.metagame.shop.dto.PaymentOption
import com.game7th.metagame.shop.dto.ShopItem
import com.game7th.swiped.api.PackEntryDto
import com.game7th.swiped.api.PaymentOptionDto
import com.game7th.swiped.api.PurchaseRequestDto

class ShopServiceImpl(
        val mapper: PurchaseItemMapper,
        val api: CloudApi
) : ShopService {

    var cache = emptyList<ShopItem>()

    var dirty = true

    override suspend fun listItems(): List<ShopItem> {
        maybeRefreshShop()
        return cache
    }

    private suspend fun maybeRefreshShop() {
        if (dirty) {
            val display = api.listShopDisplay()
            cache = mapper.mapItems(display.inAppPurchases.map { it.sku }).map {
                ShopItem.PackShopItem(it.name, "inapp_${it.id.replace("_", "")}", listOf(
                        PaymentOption.PurchaseOption(it)
                ), it.id)
            } + display.shopItems.map {
                ShopItem.PackShopItem(it.title, it.texture.replace("_", ""), it.paymentOptions.map {
                    PaymentOption.IngamePayOption(it.amount, it.currency)
                }, it.id)
            }
            dirty = false
        }
    }

    override suspend fun purchase(id: String, option: PaymentOption): List<PackEntryDto> {
        maybeRefreshShop()
        return when (option) {
            is PaymentOption.PurchaseOption -> {
                val token = mapper.purchase(id)
                if (token.isNotEmpty()) {
                    return try {
                        val rewards = api.validateGooglePurchase(token, id)
                        mapper.consume(token)
                        rewards
                    } catch (e: NetworkError) {
                        println("Failed to purchase")
                        throw e
                    }
                }
                throw NetworkError(NetworkErrorStatus.UNKNOWN_ERROR, "Empty token")
            }
            is PaymentOption.IngamePayOption -> {
                val result = api.internalPurchase(PurchaseRequestDto(id, PaymentOptionDto(option.currency, option.amount)))
                result
            }
        }
    }

    override suspend fun restorePurchase(sku: String, purchaseToken: String) {
        try {
            api.validateGooglePurchase(purchaseToken, sku)
            mapper.consume(purchaseToken)
        } catch (e: NetworkError) {
            println("Failed to restore $sku $purchaseToken")
        }
    }
}