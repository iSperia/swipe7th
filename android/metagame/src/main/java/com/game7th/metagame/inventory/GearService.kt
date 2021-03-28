package com.game7th.metagame.inventory

import com.game7th.metagame.account.RewardData
import com.game7th.metagame.inventory.dto.InventoryItem

interface GearService {
    fun getArtifactReward(level: Int): RewardData.ArtifactRewardData?
    fun addRewards(rewards: List<RewardData>)
    fun listInventory(): List<InventoryItem>
    fun equipItem(personageId: Int, item: InventoryItem)
    fun removeItem(item: InventoryItem)
    fun upgradeItem(item: InventoryItem)
}