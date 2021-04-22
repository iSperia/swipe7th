package com.game7th.metagame.shop

import com.game7th.metagame.shop.dto.PaymentOption
import com.game7th.metagame.shop.dto.ShopItem
import com.game7th.swiped.api.PackEntryDto

interface ShopService {

    suspend fun listItems(): List<ShopItem>

    suspend fun purchase(id: String, option: PaymentOption): List<PackEntryDto>

    suspend fun restorePurchase(sku: String, purchaseToken: String)
}