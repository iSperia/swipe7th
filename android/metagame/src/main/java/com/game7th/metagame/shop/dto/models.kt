package com.game7th.metagame.shop.dto

import com.game7th.metagame.account.dto.Currency
import com.game7th.metagame.inventory.dto.InventoryItem

data class PaymentOption(
        val amount: Int,
        val currency: Currency
)

sealed class ShopItem(val id: String, val paymentOptions: List<PaymentOption>) {

    class GearShopItem(
        val item: InventoryItem,
        paymentOptions: List<PaymentOption>,
        id: String
    ): ShopItem(id, paymentOptions)

    class PersonageShopItem(
            val personage: String,
            paymentOptions: List<PaymentOption>,
            id: String
    ): ShopItem(id, paymentOptions)

    class PackShopItem(
        val name: String,
        paymentOptions: List<PaymentOption>,
        id: String
    ): ShopItem(id, paymentOptions)
}