package com.game7th.metagame.campaign

import com.game7th.metagame.campaign.dto.ActConfig
import com.game7th.metagame.campaign.dto.CampaignNodeType
import com.game7th.metagame.campaign.dto.LocationConfig
import com.game7th.metagame.dto.UnitConfig
import com.game7th.metagame.dto.UnitType
import com.game7th.metagame.network.CloudApi
import com.game7th.metagame.network.NetworkError
import com.game7th.swiped.api.LocationCompleteResponseDto
import com.game7th.swiped.api.LocationType

/**
 * View model for campaign stuff
 */
class ActsServiceImpl(
        private val api: CloudApi
) : ActsService {

    val actCache = mutableMapOf<String, ActConfig>()

    @Throws(NetworkError::class)
    override suspend fun getActConfig(name: String): ActConfig {
        if (actCache.containsKey(name)) return actCache[name]!!

        val config = reloadActConfig(name)

        return config
    }

    private suspend fun reloadActConfig(actName: String): ActConfig {
        val act = api.getAct(actName)
        val progress = api.getActProgress(actName)
        val unlockedLocations = mutableSetOf<Int>()
        unlockedLocations.add(0)
        progress.forEach { progress ->
            unlockedLocations.add(progress.locationId)
            act.locations.firstOrNull { it.index == progress.locationId }?.let { location ->
                unlockedLocations.addAll(location.unlock)
            }
        }

        val config = ActConfig(act.texture, act.locations.map { location ->
            LocationConfig(location.index, when (location.locationType) {
                    LocationType.FARM -> CampaignNodeType.FARM
                    else -> CampaignNodeType.REGULAR
            }, location.x.toFloat(), location.y.toFloat(), location.unlock, location.waves.map {
                    it.monsters.map { UnitConfig(UnitType.valueOf(it.name), it.level) }
                }, location.scene, !unlockedLocations.contains(location.index), progress.firstOrNull { it.locationId == location.index }?.timeoutStarted ?: 0, location.farm)
            })
        actCache.put(actName, config)
        return config
    }

    override suspend fun markLocationComplete(name: String, locationId: Int, personageId: String): LocationCompleteResponseDto {
        val rewards = api.markLocationComplete(name, locationId, personageId)

        reloadActConfig(name)

        return rewards
    }
}