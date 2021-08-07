package com.game7th.metagame.campaign.dto

import com.game7th.metagame.dto.UnitConfig

enum class CampaignNodeType {
    REGULAR, BOSS, FARM
}

data class LocationConfig(
        val id: Int,
        val type: CampaignNodeType,
        val x: Float,
        val y: Float,
        val unlock: List<Int>,
        val waves: List<List<UnitConfig>> = mutableListOf(),
        val scene: String,
        val isLocked: Boolean
)

data class ActConfig(
        val texture: String,
        val nodes: List<LocationConfig>
) {
    fun findNode(id: Int): LocationConfig? = nodes.firstOrNull { it.id == id }
}