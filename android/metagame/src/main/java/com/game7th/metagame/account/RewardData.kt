package com.game7th.metagame.account

import com.game7th.metagame.inventory.InventoryItem

sealed class RewardData {

    data class ArtifactRewardData(val item: InventoryItem) : RewardData()
}