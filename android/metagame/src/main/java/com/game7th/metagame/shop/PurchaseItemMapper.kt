package com.game7th.metagame.shop

interface PurchaseItemMapper {
    suspend fun mapItems(ids: List<String>): List<PurchaseItemInfo>
    suspend fun purchase(id: String): String
    suspend fun consume(token: String): Unit
}