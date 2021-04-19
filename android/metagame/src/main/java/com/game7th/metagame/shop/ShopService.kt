package com.game7th.metagame.shop

import com.game7th.metagame.shop.dto.ShopItem

interface ShopService {

    suspend fun listItems(): List<ShopItem>

    suspend fun purchase(id: String): Boolean

    suspend fun restorePurchase(sku: String, purchaseToken: String)
}