package com.game7th.metagame.shop

interface PurchaseItemMapper {
    suspend fun mapItems(ids: List<String>): List<PurchaseItemInfo>
}