package com.game7th.swipe

import android.content.Context
import com.game7th.swipe.state.CampaignState
import com.game7th.swipe.state.GameState
import com.game7th.swipe.state.LocationState
import com.game7th.swipe.state.StateStorage
import com.google.gson.Gson

class StateStorageImpl(context: Context) : StateStorage {

    private val prefs = context.getSharedPreferences(PREFS_NAME, 0)

    private val gson = Gson()

    override fun save(state: GameState) {
        val stateString = gson.toJson(state)
        prefs.edit().putString(PREF_STATE, stateString).apply()
    }

    override fun updateCampaign(id: Int, state: CampaignState) {
        val gameState = load()
        gameState.campaigns.filter { it.id != id }
                .plus(state)
                .let { campaigns ->
                    save(gameState.copy(campaigns = campaigns))
                }
    }

    override fun load(): GameState {
        val stateString = prefs.getString(PREF_STATE, null)
        return stateString?.let {
            gson.fromJson<GameState>(stateString, GameState::class.java)
        } ?: createDefaultState()
    }

    private fun createDefaultState(): GameState {
        return GameState(listOf(
                CampaignState(0, listOf(
                        LocationState(0, 0)
                ))
        ))
    }

    companion object {
        const val PREFS_NAME = "storage"
        const val PREF_STATE = "state"
    }
}