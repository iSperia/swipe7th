package com.game7th.metagame.campaign

import com.game7th.metagame.unit.UnitConfig

enum class CampaignNodeType {
    REGULAR, BOSS, FARM
}

data class LocationConfig(
        val id: Int,
        val type: CampaignNodeType,
        val x: Float,
        val y: Float,
        val unlock: List<Int>,
        val waves: List<List<UnitConfig>> = mutableListOf()
)

data class ActConfig(
        val texture: String,
        val nodes: List<LocationConfig>
) {
    fun findNode(id: Int): LocationConfig? = nodes.firstOrNull { it.id == id }
}