package com.game7th.metagame.campaign

import com.game7th.metagame.PersistentStorage
import com.game7th.metagame.state.ActProgressState
import com.game7th.metagame.state.LocationProgressState
import com.google.gson.Gson

/**
 * View model for campaign stuff
 */
class ActsServiceImpl(
        private val gson: Gson,
        private val config: ActConfig,
        private val storage: PersistentStorage
) : ActsService {

    override fun getActConfig(id: Int): ActConfig {
        //TODO: Add act storage access when we have 2+ campaigns
        return config
    }

    override fun getActProgress(id: Int): ActProgressState {
        //TODO: check if act is locked or not
        val data = storage.get("$STATE_ACT-$id")
        val stateFromStorage = data?.let { gson.fromJson<ActProgressState>(it, ActProgressState::class.java) }
        return stateFromStorage ?: createDefaultActProgress(id)
    }

    override fun markLocationComplete(actId: Int, locationId: Int, starCount: Int): Boolean {
        val currentState = getActProgress(actId)
        currentState.locations
                .asSequence()
                .filter { it.id != locationId }
                .plus(LocationProgressState(locationId, starCount))
                .toList()
                .let { ActProgressState(actId, it) }
                .let {
                    storage.put("$STATE_ACT-$actId", gson.toJson(it))
                }
        return true
    }

    override fun unlockLocation(actId: Int, locationId: Int): Boolean {
        val currentState = getActProgress(actId)
        val isLocked = currentState.locations.count { it.id == locationId } == 0
        if (isLocked) {
            currentState.locations
                    .plus(LocationProgressState(locationId, 0))
                    .let { ActProgressState(actId, it) }
                    .let {
                        storage.put("$STATE_ACT-$actId", gson.toJson(it))
                    }
        }
        return true
    }

    private fun createDefaultActProgress(id: Int) = ActProgressState(
            id = id,
            locations = listOf(LocationProgressState(0, 0)))

    companion object {
        const val STATE_ACT = "acts_progress"
    }
}