package com.game7th.metagame

import com.game7th.metagame.campaign.ActsService

/**
 * The entity for everything happens in the metagame:
 * personages, campaigns, stash, curency.
 * This one is big service locator actually
 */
class GameService(
        private val actsService: ActsService
) : ActsService by actsService