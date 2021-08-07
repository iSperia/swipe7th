package com.game7th.metagame.inventory

import com.game7th.swiped.api.FlaskItemFullInfoDto
import com.game7th.swiped.api.InventoryItemFullInfoDto

interface GearService {
    suspend fun listInventory(): List<InventoryItemFullInfoDto>
    suspend fun equipItem(personageId: String, item: InventoryItemFullInfoDto)
    suspend fun dequipItem(personageId: String, item: InventoryItemFullInfoDto)
    suspend fun reloadData()
    suspend fun listFlasks(): List<FlaskItemFullInfoDto>
    suspend fun consumeFlask(flaskItemId: String)
}