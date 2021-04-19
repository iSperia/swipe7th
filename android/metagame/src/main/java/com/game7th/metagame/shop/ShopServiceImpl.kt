package com.game7th.metagame.shop

import com.game7th.metagame.shop.dto.PaymentOption
import com.game7th.metagame.shop.dto.ShopItem

class ShopServiceImpl(
        val mapper: PurchaseItemMapper
) : ShopService {

    override suspend fun listItems(): List<ShopItem> {
        return mapper.mapItems(listOf("crystal_100", "crystal_500")).map {
            ShopItem.PackShopItem(it.name, listOf(
                    PaymentOption.PurchaseOption(it)
            ), it.id)
        }
    }
}