package com.game7th.metagame.shop.dto

import com.game7th.swiped.api.Currency
import com.game7th.swiped.api.InventoryItemFullInfoDto

data class PaymentOption(
        val amount: Int,
        val currency: Currency
)

sealed class ShopItem(val id: String, val paymentOptions: List<PaymentOption>) {

    class GearShopItem(
        val item: InventoryItemFullInfoDto,
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