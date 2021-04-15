package com.game7th.metagame.campaign

import com.game7th.metagame.campaign.dto.ActConfig
import com.game7th.metagame.campaign.dto.CampaignNodeType
import com.game7th.metagame.campaign.dto.LocationConfig
import com.game7th.metagame.dto.ActProgressState
import com.game7th.metagame.dto.LocationProgressState
import com.game7th.metagame.dto.UnitConfig
import com.game7th.metagame.dto.UnitType
import com.game7th.metagame.network.CloudApi
import com.game7th.metagame.network.NetworkError
import com.game7th.swiped.api.LocationCompleteResponseDto

/**
 * View model for campaign stuff
 */
class ActsServiceImpl(
        private val api: CloudApi
) : ActsService {

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

        val config = reloadActProgress(name)

        return config
    }

    private suspend fun reloadActProgress(actName: String): ActConfig {
        val act = api.getAct(actName)
        val config = ActConfig(act.texture, act.locations.mapIndexed { index, location ->
            LocationConfig(index, CampaignNodeType.REGULAR, location.x.toFloat(), location.y.toFloat(), location.unlock, location.waves.map {
                it.monsters.map { UnitConfig(UnitType.valueOf(it.name), it.level) }
            })
        })
        actCache.put(actName, config)
        return config
    }

    override suspend fun getActProgress(actName: String): ActProgressState {
        progressCache[actName]?.let { return it }

        val progress = api.getActProgress(actName)
        val state = ActProgressState(actName, progress.map { LocationProgressState(it.locationId, it.starsComplete) })

        progressCache[actName] = state
        return state
    }

    override suspend fun markLocationComplete(name: String, locationId: Int, starCount: Int, personageId: String): LocationCompleteResponseDto {
        val rewards = api.markLocationComplete(name, locationId, starCount, personageId)

        reloadActProgress(name)

        return rewards
    }
}