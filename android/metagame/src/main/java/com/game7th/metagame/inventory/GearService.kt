package com.game7th.metagame.inventory

import com.game7th.metagame.account.RewardData
import com.game7th.metagame.inventory.dto.FlaskStackDto
import com.game7th.metagame.inventory.dto.FlaskTemplate
import com.game7th.metagame.inventory.dto.InventoryItem

interface GearService {
    fun getArtifactReward(level: Int): RewardData.ArtifactRewardData?
    fun addRewards(rewards: List<RewardData>)
    fun listInventory(): List<InventoryItem>
    fun equipItem(personageId: Int, item: InventoryItem)
    fun removeItem(item: InventoryItem)
    fun upgradeItem(item: InventoryItem)
    fun listFlasks(): List<FlaskStackDto>
    fun removeFlask(flask: FlaskTemplate)
    fun addFlask(flask: FlaskTemplate)
    fun generateFlaskReward(): RewardData.FlaskRewardData?
}