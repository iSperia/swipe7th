package com.game7th.metagame.shop

import com.game7th.metagame.shop.dto.PaymentOption
import com.game7th.metagame.shop.dto.ShopItem

interface ShopService {

    fun listItems(): List<ShopItem>

    fun acquireItem(id: String, paymentOption: PaymentOption): Boolean
}