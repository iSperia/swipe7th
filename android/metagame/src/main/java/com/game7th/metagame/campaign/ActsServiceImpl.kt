package com.game7th.metagame.campaign

import com.game7th.metagame.FileProvider
import com.game7th.metagame.PersistentStorage
import com.game7th.metagame.account.AccountService
import com.game7th.metagame.account.RewardData
import com.game7th.metagame.account.dto.Currency
import com.game7th.metagame.campaign.dto.ActConfig
import com.game7th.metagame.inventory.GearService
import com.game7th.metagame.dto.ActProgressState
import com.game7th.metagame.dto.LocationProgressState
import com.game7th.metagame.dto.UnitConfig
import com.game7th.metagame.dto.UnitType
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.google.gson.Gson
import kotlin.math.min
import kotlin.random.Random

/**
 * View model for campaign stuff
 */
class ActsServiceImpl(
        private val gson: Gson,
        private val storage: PersistentStorage,
        private val fileProvider: FileProvider,
        private val gearService: GearService,
        private val accountService: AccountService
) : ActsService {

    val csvReader = csvReader()

    val progressCache = mutableMapOf<Int, ActProgressState>()

    val actCache = mutableMapOf<Int, ActConfig>()

    private data class NpcEntry(
            val level: Int,
            val wave: Int,
            val order: Int,
            val unitType: UnitType,
            val unitLevel: Int
    )

    override fun getActConfig(id: Int): ActConfig {
        if (actCache.containsKey(id)) return actCache[id]!!

        val actConfig = gson.fromJson<ActConfig>(
                fileProvider.getFileContent("campaign_$id.json"),
                ActConfig::class.java)

        val npcEntries = csvReader.readAllWithHeader(fileProvider.getFileContent("act_$id.csv")!!).map {
            NpcEntry(
                    level = it["level"]!!.toInt() - 1,
                    wave = it["wave"]!!.toInt(),
                    order = it["order"]!!.toInt(),
                    unitType = UnitType.valueOf(it["unit_type"]!!),
                    unitLevel = it["unit_level"]!!.toInt()
            )
        }

        val npcByLevel = npcEntries.groupBy { it.level }

        val config = actConfig.copy(texture = actConfig.texture,
                nodes = actConfig.nodes.map { locationConfig ->
                    if (npcByLevel.containsKey(locationConfig.id)) {
                        locationConfig.copy(waves =
                        npcByLevel[locationConfig.id]!!
                                .groupBy { it.wave }
                                .toSortedMap()
                                .entries
                                .map {
                                    it.value.sortedBy { it.order }.map { entry ->
                                        UnitConfig(entry.unitType, entry.unitLevel)
                                    }
                                })
                    } else locationConfig
                })
        actCache[id] = config
        return config
    }

    override fun getActProgress(id: Int): ActProgressState {
        //TODO: check if act is locked or not
        progressCache[id]?.let { return it }

        val data = storage.get("$STATE_ACT-$id")
        val stateFromStorage = data?.let { gson.fromJson<ActProgressState>(it, ActProgressState::class.java) }
        val result = stateFromStorage ?: createDefaultActProgress(id)
        progressCache[id] = result
        return result
    }

    override fun markLocationComplete(actId: Int, locationId: Int, starCount: Int): List<RewardData> {
        val currentState = getActProgress(actId)
        val prevStars = currentState.locations.firstOrNull { it.id == locationId }?.stars
        if ((prevStars ?: 0) >= starCount) return emptyList()

        var actProgressState = currentState.locations
                .asSequence()
                .filter { it.id != locationId }
                .plus(LocationProgressState(locationId, starCount))
                .toList()
                .let { ActProgressState(actId, it) }

        getActConfig(actId).nodes.firstOrNull { it.id == locationId }?.let { location ->
            location.unlock.forEach { unlockLocationId ->
                actProgressState = unlockLocation(actProgressState, actId, unlockLocationId)
            }
        }

        progressCache[actId] = actProgressState
        storage.put("$STATE_ACT-$actId", gson.toJson(actProgressState))

        //ok, we have some rewards
        val config = getActConfig(actId)
        val location = config.nodes.firstOrNull { it.id == locationId }
        location?.let {
            val totalPoints = it.waves.flatten().sumBy { it.level + (starCount - 1) * 3  }
            val maxArtifactLevel = it.waves.flatten().maxBy { it.level }?.level ?: 1 + (starCount - 1) * 3

            val rewards = mutableListOf<RewardData>()
            val r1 = min(maxArtifactLevel, Random.nextInt(totalPoints) + 1)
            gearService.getArtifactReward(r1)?.let { rewards.add(it) }
            val goldAmount = (totalPoints - r1) * 100
            rewards.add(RewardData.CurrencyRewardData(Currency.GOLD, goldAmount))

            gearService.addRewards(rewards)
            accountService.addRewards(rewards)
            return rewards
        }

        return emptyList()
    }

    override fun unlockLocation(currentState: ActProgressState, actId: Int, locationId: Int): ActProgressState {
        val isLocked = currentState.locations.count { it.id == locationId } == 0
        if (isLocked) {
            return currentState.locations
                    .plus(LocationProgressState(locationId, 0))
                    .let { ActProgressState(actId, it) }
        }
        return currentState
    }

    private fun createDefaultActProgress(id: Int) = ActProgressState(
            id = id,
            locations = listOf(LocationProgressState(0, 0)))

    companion object {
        const val STATE_ACT = "acts_progress"
    }
}