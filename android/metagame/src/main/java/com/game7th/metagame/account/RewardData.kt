package com.game7th.metagame.account

import com.game7th.metagame.account.dto.Currency
import com.game7th.metagame.inventory.dto.InventoryItem

sealed class RewardData {

    data class ArtifactRewardData(val item: InventoryItem) : RewardData()

    data class CurrencyRewardData(val currency: Currency, val amount: Int): RewardData()
}