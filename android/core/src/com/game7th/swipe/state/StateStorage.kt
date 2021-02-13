package com.game7th.swipe.state

interface StateStorage {

    fun updateCampaign(id: Int, state: CampaignState)

    fun save(state: GameState)

    fun load(): GameState
}