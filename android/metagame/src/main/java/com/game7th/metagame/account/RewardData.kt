package com.game7th.metagame.account

import com.game7th.metagame.inventory.dto.InventoryItem

sealed class RewardData {

    data class ArtifactRewardData(val item: InventoryItem) : RewardData()
}