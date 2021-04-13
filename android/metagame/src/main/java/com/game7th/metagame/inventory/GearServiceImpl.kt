package com.game7th.metagame.inventory

import com.game7th.metagame.FileProvider
import com.game7th.metagame.PersistentStorage
import com.game7th.metagame.account.RewardData
import com.game7th.metagame.inventory.dto.*
import com.game7th.swiped.api.InventoryItemFullInfoDto
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
                    items = mutableListOf(),
                    flasks = mutableListOf(FlaskStackDto(FlaskTemplate("LIFE_FLASK_SMALL", 100, 0), 5), FlaskStackDto(FlaskTemplate("LIFE_FLASK_MEDIUM", 200,0), 5))
            )
            initialData
        } else {
            gson.fromJson<InventoryPool>(inventoryString, InventoryPool::class.java)
        }

        gearConfig = gson.fromJson<GearConfig>(files.getFileContent("artifacts.json"), GearConfig::class.java)
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

    override fun equipItem(personageId: Int, item: InventoryItemFullInfoDto) {

    }

    override fun removeItem(item: InventoryItemFullInfoDto) {
        inventory.items.remove(item)
        storage.put(KEY_INVENTORY, gson.toJson(inventory))
    }

    override fun upgradeItem(item: InventoryItemFullInfoDto) {
        val newItem = item.copy(level = item.level + 1)
        inventory.items.remove(item)
        inventory.items.add(newItem)
    }

    override fun listFlasks(): List<FlaskStackDto> {
        return inventory.flasks
    }

    override fun removeFlask(flask: FlaskTemplate) {
        val stack = inventory.flasks.firstOrNull { it.template == flask }
        if (stack != null) {
            if (stack.amount == 1) {
                inventory.flasks.remove(stack)
            } else {
                stack.amount--
            }
        }
        storage.put(KEY_INVENTORY, gson.toJson(inventory))
    }

    override fun addFlask(flask: FlaskTemplate) {
        val stack = inventory.flasks.firstOrNull { it.template == flask }
        if (stack == null) {
            inventory.flasks.add(FlaskStackDto(flask, 1))
        } else {
            stack.amount++
        }
        storage.put(KEY_INVENTORY, gson.toJson(inventory))
    }

    override fun generateFlaskReward(): RewardData.FlaskRewardData? {
        val filteredFlasks = gearConfig.flasks
        val totalWeight = filteredFlasks.sumBy { it.dropWeight }
        val roll = Random.nextInt(1, totalWeight + 1)
        var sum = 0
        return filteredFlasks.firstOrNull {
            sum += it.dropWeight
            sum >= roll
        }?.let {
            RewardData.FlaskRewardData(it.template.copy())
        }
    }

    companion object {
        const val KEY_INVENTORY = "account.inventory"
    }

}