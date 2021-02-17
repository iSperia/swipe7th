package com.game7th.metagame.campaign

import com.game7th.metagame.FileProvider
import com.game7th.metagame.PersistentStorage
import com.game7th.metagame.state.ActProgressState
import com.game7th.metagame.state.LocationProgressState
import com.game7th.metagame.unit.UnitConfig
import com.game7th.metagame.unit.UnitType
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.google.gson.Gson

/**
 * View model for campaign stuff
 */
class ActsServiceImpl(
        private val gson: Gson,
        private val storage: PersistentStorage,
        private val fileProvider: FileProvider
) : ActsService {

    val csvReader = csvReader()

    val progressCache = mutableMapOf<Int, ActProgressState>()

    private data class NpcEntry(
            val level: Int,
            val wave: Int,
            val order: Int,
            val unitType: UnitType,
            val unitLevel: Int
    )

    override fun getActConfig(id: Int): ActConfig {
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

    override fun markLocationComplete(actId: Int, locationId: Int, starCount: Int): Boolean {
        val currentState = getActProgress(actId)
        val prevStars = currentState.locations.firstOrNull { it.id == locationId }?.stars
        if ((prevStars ?: 0) > starCount) return false

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

        return true
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