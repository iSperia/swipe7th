package com.game7th.metagame.campaign

enum class CampaignNodeType {
    REGULAR, BOSS, FARM
}

data class CampaignNodeConfig(
        val id: Int,
        val type: CampaignNodeType,
        val x: Float,
        val y: Float,
        val unlock: List<Int>
)

data class CampaignConfig(
        val texture: String,
        val nodes: List<CampaignNodeConfig>
) {
    fun findNode(id: Int): CampaignNodeConfig? = nodes.firstOrNull { it.id == id }
}