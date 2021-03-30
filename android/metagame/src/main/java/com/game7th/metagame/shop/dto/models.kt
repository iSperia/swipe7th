package com.game7th.metagame.shop.dto

import com.game7th.metagame.account.dto.Currency
import com.game7th.metagame.dto.UnitType
import com.game7th.metagame.inventory.dto.InventoryItem

data class PaymentOption(
        val amount: Int,
        val currency: Currency
)

sealed class ShopItem(val id: String) {

    class GearShopItem(
        val item: InventoryItem,
        val paymentOptions: List<PaymentOption>,
        id: String
    ): ShopItem(id)

    class PersonageShopItem(
            val personage: UnitType,
            val paymentOptions: List<PaymentOption>,
            id: String
    ): ShopItem(id)
}