package com.game7th.metagame.shop

import com.game7th.metagame.network.CloudApi
import com.game7th.metagame.network.NetworkError
import com.game7th.metagame.shop.dto.PaymentOption
import com.game7th.metagame.shop.dto.ShopItem

class ShopServiceImpl(
        val mapper: PurchaseItemMapper,
        val api: CloudApi
) : ShopService {

    override suspend fun listItems(): List<ShopItem> {
        return mapper.mapItems(listOf("crystals_100", "crystals_500")).map {
            ShopItem.PackShopItem(it.name, listOf(
                    PaymentOption.PurchaseOption(it)
            ), it.id)
        }
    }

    override suspend fun purchase(id: String): Boolean {
        val token = mapper.purchase(id)
        if (token.isNotEmpty()) {
            return try {
                api.validateGooglePurchase(token, id)
                mapper.consume(token)
                true
            } catch (e: NetworkError) {
                println("Failed to purchase")
                false
            }
        }
        return false
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