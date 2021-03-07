package com.game7th.metagame.inventory

import com.game7th.metagame.account.RewardData

interface GearService {
    fun getArtifactReward(level: Int): RewardData.ArtifactRewardData?
    fun addRewards(rewards: List<RewardData>)
}