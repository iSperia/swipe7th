package com.game7th.metagame.campaign

import com.game7th.metagame.FileProvider
import com.game7th.metagame.PersistentStorage
import com.game7th.metagame.account.AccountService
import com.game7th.metagame.account.RewardData
import com.game7th.metagame.campaign.dto.ActConfig
import com.game7th.metagame.campaign.dto.CampaignNodeType
import com.game7th.metagame.campaign.dto.LocationConfig
import com.game7th.metagame.inventory.GearService
import com.game7th.metagame.dto.ActProgressState
import com.game7th.metagame.dto.LocationProgressState
import com.game7th.metagame.dto.UnitConfig
import com.game7th.metagame.dto.UnitType
import com.game7th.metagame.network.CloudApi
import com.game7th.metagame.network.NetworkError
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.google.gson.Gson

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

    override suspend fun getActProgress(actName: String): ActProgressState {
        progressCache[actName]?.let { return it }

        val progress = api.getActProgress(actName)
        val state = ActProgressState(actName, progress.map { LocationProgressState(it.locationId, it.starsComplete) })

        progressCache[actName] = state
        return state
    }

    override suspend fun markLocationComplete(name: String, locationId: Int, starCount: Int): List<RewardData> {
        val rewards = api.markLocationComplete(name, locationId, starCount)

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

        val result = mutableListOf<RewardData>()

        result.addAll(rewards.gear.map { RewardData.ArtifactRewardData(it.item) })
        result.addAll(rewards.currency.map { RewardData.CurrencyRewardData(it.currency, it.amount) })

        return result
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

    companion object {
        const val STATE_ACT = "acts_progress"
    }
}