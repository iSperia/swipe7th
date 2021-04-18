package com.game7th.metagame.inventory

import com.game7th.metagame.inventory.dto.FlaskStackDto
import com.game7th.metagame.inventory.dto.FlaskTemplate
import com.game7th.swiped.api.InventoryItemFullInfoDto

interface GearService {
    suspend fun listInventory(): List<InventoryItemFullInfoDto>
    suspend fun equipItem(personageId: String, item: InventoryItemFullInfoDto)
    suspend fun dequipItem(personageId: String, item: InventoryItemFullInfoDto)
    suspend fun reloadData()
    suspend fun dustItem(item: InventoryItemFullInfoDto): Boolean
    suspend fun pumpItem(item: InventoryItemFullInfoDto): Boolean
    fun listFlasks(): List<FlaskStackDto>
    fun removeFlask(flask: FlaskTemplate)
    fun addFlask(flask: FlaskTemplate)
}