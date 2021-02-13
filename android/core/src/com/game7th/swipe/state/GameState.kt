package com.game7th.swipe.state

data class LocationState(
    val id: Int,
    val stars: Int
)

data class CampaignState(
        val id: Int,
        val locations: List<LocationState>
)

data class GameState(
        val campaigns: List<CampaignState>
)