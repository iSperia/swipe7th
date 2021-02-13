package com.game7th.metagame.campaign

import com.game7th.metagame.state.ActProgressState

interface ActsService {

    fun getActConfig(id: Int): ActConfig

    fun getActProgress(id: Int): ActProgressState

    /**
     * Mark user completed some location with some stars involved
     */
    fun markLocationComplete(actId: Int, locationId: Int, starCount: Int): Boolean

    /**
     * Unlocks location so player may play this level
     */
    fun unlockLocation(actId: Int, locationId: Int): Boolean
}