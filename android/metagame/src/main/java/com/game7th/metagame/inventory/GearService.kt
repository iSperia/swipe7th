package com.game7th.metagame.inventory

import com.game7th.metagame.account.RewardData
import com.game7th.metagame.inventory.dto.FlaskStackDto
import com.game7th.metagame.inventory.dto.FlaskTemplate
import com.game7th.swiped.api.InventoryItemFullInfoDto

interface GearService {
    fun addRewards(rewards: List<RewardData>)
    fun listInventory(): List<InventoryItemFullInfoDto>
    fun equipItem(personageId: Int, item: InventoryItemFullInfoDto)
    fun removeItem(item: InventoryItemFullInfoDto)
    fun upgradeItem(item: InventoryItemFullInfoDto)
    fun listFlasks(): List<FlaskStackDto>
    fun removeFlask(flask: FlaskTemplate)
    fun addFlask(flask: FlaskTemplate)
    fun generateFlaskReward(): RewardData.FlaskRewardData?
}