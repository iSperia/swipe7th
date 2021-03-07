package com.game7th.metagame.inventory

import com.game7th.metagame.FileProvider
import com.game7th.metagame.PersistentStorage
import com.game7th.metagame.account.RewardData
import com.google.gson.Gson
import kotlin.random.Random

class GearServiceImpl(
        private val gson: Gson,
        private val storage: PersistentStorage,
        private val files: FileProvider
) : GearService {

    val gearConfig: GearConfig

    var inventory: InventoryPool

    init {
        val inventoryString = storage.get(KEY_INVENTORY)
        inventory = if (inventoryString == null) {
            val initialData = InventoryPool(
                    items = (1..10).map { InventoryItem(gbFlatArmor = it, level = it, node = ItemNode.BODY, name = "SHIELD") }.toMutableList()
            )
            initialData
        } else {
            gson.fromJson<InventoryPool>(inventoryString, InventoryPool::class.java)
        }

        gearConfig = gson.fromJson<GearConfig>(files.getFileContent("artifacts.json"), GearConfig::class.java)
    }

    override fun getArtifactReward(level: Int): RewardData.ArtifactRewardData? {
        val filteredArtifacts = gearConfig.items.filter { it.maxLevel >= level && it.minLevel <= level }
        val totalWeight = filteredArtifacts.sumBy { it.weight }
        val roll = Random.nextInt(1, totalWeight + 1)
        var sum = 0
        return filteredArtifacts.firstOrNull {
            sum += it.weight
            sum >= roll
        }?.let {
            RewardData.ArtifactRewardData(it.template.copy(level = level))
        }
    }

    override fun addRewards(rewards: List<RewardData>) {
        rewards.forEach {
            when (it) {
                is RewardData.ArtifactRewardData -> {
                    inventory.items.add(it.item)
                }
            }
        }
        storage.put(KEY_INVENTORY, gson.toJson(inventory)) //save inventory to storage
    }

    override fun listInventory() = inventory.items

    companion object {
        const val KEY_INVENTORY = "account.inventory"
    }

}