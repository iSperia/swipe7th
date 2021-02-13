package com.game7th.metagame.campaign

/**
 * View model for campaign stuff
 */
class CampaignViewModel(private val config: CampaignConfig) {

    val nodeConfigs = mutableListOf<CampaignNodeConfig>()

    var texture: String = config.texture

    init {
        nodeConfigs.addAll(config.nodes)
    }
}