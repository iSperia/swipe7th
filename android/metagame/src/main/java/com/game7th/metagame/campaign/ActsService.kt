package com.game7th.metagame.campaign

import com.game7th.metagame.campaign.dto.ActConfig
import com.game7th.metagame.dto.ActProgressState
import com.game7th.metagame.network.NetworkError
import com.game7th.swiped.api.LocationCompleteResponseDto

interface ActsService {

    @Throws(NetworkError::class)
    suspend fun getActConfig(actName: String): ActConfig

    suspend fun getActProgress(actName: String): ActProgressState

    /**
     * Mark user completed some location with some stars involved
     */
    suspend fun markLocationComplete(actName: String, locationId: Int, starCount: Int, personageId: String): LocationCompleteResponseDto
}