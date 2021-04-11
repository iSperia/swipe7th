package com.game7th.metagame.campaign

import com.game7th.metagame.account.RewardData
import com.game7th.metagame.campaign.dto.ActConfig
import com.game7th.metagame.dto.ActProgressState
import com.game7th.metagame.network.NetworkError

interface ActsService {

    @Throws(NetworkError::class)
    suspend fun getActConfig(actName: String): ActConfig

    suspend fun getActProgress(actName: String): ActProgressState

    /**
     * Mark user completed some location with some stars involved
     */
    suspend fun markLocationComplete(actName: String, locationId: Int, starCount: Int): List<RewardData>

    /**
     * Unlocks location so player may play this level
     */
    suspend fun unlockLocation(currentState: ActProgressState, actName: String, locationId: Int): ActProgressState
}