package com.game7th.metagame.campaign

import com.game7th.metagame.FileProvider
import com.game7th.metagame.PersistentStorage
import com.game7th.metagame.account.AccountService
import com.game7th.metagame.account.RewardData
import com.game7th.metagame.account.dto.Currency
import com.game7th.metagame.campaign.dto.ActConfig
import com.game7th.metagame.campaign.dto.CampaignNodeType
import com.game7th.metagame.campaign.dto.LocationConfig
import com.game7th.metagame.inventory.GearService
import com.game7th.metagame.dto.ActProgressState
import com.game7th.metagame.dto.LocationProgressState
import com.game7th.metagame.dto.UnitConfig
import com.game7th.metagame.dto.UnitType
import com.game7th.metagame.inventory.dto.InventoryItem
import com.game7th.metagame.inventory.dto.ItemNode
import com.game7th.metagame.network.CloudApi
import com.game7th.metagame.network.NetworkError
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.google.gson.Gson
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * View model for campaign stuff
 */
class ActsServiceImpl(
        private val gson: Gson,
        private val api: CloudApi,
        private val storage: PersistentStorage,
        private val fileProvider: FileProvider,
        private val gearService: GearService,
        private val accountService: AccountService
) : ActsService {

    val csvReader = csvReader()

    val progressCache = mutableMapOf<String, ActProgressState>()

    val actCache = mutableMapOf<String, ActConfig>()

    private data class NpcEntry(
            val level: Int,
            val wave: Int,
            val order: Int,
            val unitType: UnitType,
            val unitLevel: Int
    )

    @Throws(NetworkError::class)
    override suspend fun getActConfig(name: String): ActConfig {
        if (actCache.containsKey(name)) return actCache[name]!!

        val act = api.getAct(name)
        val config =  ActConfig(act.texture, act.locations.mapIndexed { index, location ->
            LocationConfig(index, CampaignNodeType.REGULAR, location.x.toFloat(), location.y.toFloat(), location.unlock, location.waves.map {
                it.monsters.map { UnitConfig(UnitType.valueOf(it.name), it.level) }
            })
        })
        actCache.put(name, config)

        return config
    }

    override suspend fun getActProgress(name: String): ActProgressState {
        //TODO: check if act is locked or not
        progressCache[name]?.let { return it }

        val data = storage.get("$STATE_ACT-$name")
        val stateFromStorage = data?.let { gson.fromJson<ActProgressState>(it, ActProgressState::class.java) }
        val result = stateFromStorage ?: createDefaultActProgress(name)
        progressCache[name] = result
        return result
    }

    override suspend fun markLocationComplete(name: String, locationId: Int, starCount: Int): List<RewardData> {
        val currentState = getActProgress(name)
        val prevStars = currentState.locations.firstOrNull { it.id == locationId }?.stars
        if ((prevStars ?: 0) >= starCount) return emptyList()

        var actProgressState = currentState.locations
                .asSequence()
                .filter { it.id != locationId }
                .plus(LocationProgressState(locationId, starCount))
                .toList()
                .let { ActProgressState(name, it) }

        getActConfig(name).nodes.firstOrNull { it.id == locationId }?.let { location ->
            location.unlock.forEach { unlockLocationId ->
                actProgressState = unlockLocation(actProgressState, name, unlockLocationId)
            }
        }

        progressCache[name] = actProgressState
        storage.put("$STATE_ACT-$name", gson.toJson(actProgressState))

        //ok, we have some rewards
        val config = getActConfig(name)
        val location = config.nodes.firstOrNull { it.id == locationId }
        location?.let {
            val totalPoints = it.waves.flatten().sumBy { it.level + (starCount - 1) * 3  }
            val maxArtifactLevel = it.waves.flatten().maxBy { it.level }?.level ?: 1 + (starCount - 1) * 3

            val rewards = mutableListOf<RewardData>()
            val r1 = if (name == "act_0" && locationId == 0 && starCount == 1) {
                rewards.add(RewardData.ArtifactRewardData(InventoryItem(gbFlatBody = 1, level = 1, node = ItemNode.FOOT, rarity = 0, name = "LEGGINGS")))
                1
            } else {
                val r1 = min(maxArtifactLevel, Random.nextInt(totalPoints) + 1)
                gearService.getArtifactReward(r1)?.let { rewards.add(it) }
                r1
            }
            val goldAmount = max(100, (totalPoints - r1) * 100)
            if (goldAmount > 0) {
                rewards.add(RewardData.CurrencyRewardData(Currency.GOLD, goldAmount))
            }

            gearService.addRewards(rewards)
            accountService.addRewards(rewards)
            return rewards
        }

        return emptyList()
    }

    override suspend fun unlockLocation(currentState: ActProgressState, name: String, locationId: Int): ActProgressState {
        val isLocked = currentState.locations.count { it.id == locationId } == 0
        if (isLocked) {
            return currentState.locations
                    .plus(LocationProgressState(locationId, 0))
                    .let { ActProgressState(name, it) }
        }
        return currentState
    }

    private fun createDefaultActProgress(name: String) = ActProgressState(
            name = name,
            locations = listOf(LocationProgressState(0, 0)))

    companion object {
        const val STATE_ACT = "acts_progress"
    }
}