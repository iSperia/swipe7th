package com.game7th.metagame.shop.dto

import com.game7th.metagame.shop.PurchaseItemInfo
import com.game7th.swiped.api.Currency
import com.game7th.swiped.api.InventoryItemFullInfoDto

sealed class PaymentOption {
    data class IngamePayOption(
            val amount: Int,
            val currency: Currency
    ): PaymentOption() {
        override fun getActionTitle() = amount.toString()
        override fun getActionTexture() = "ui_currency_gold"
        override fun getActionCurrency() = currency
    }

    data class PurchaseOption(
            val item: PurchaseItemInfo
    ): PaymentOption() {
        override fun getActionTitle() = "${item.price} ${item.priceCurrency}"
        override fun getActionTexture() = "ui_currency_gold"
        override fun getActionCurrency(): Currency? = null
    }

    abstract fun getActionTitle(): String
    abstract fun getActionTexture(): String
    abstract fun getActionCurrency(): Currency?
}

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