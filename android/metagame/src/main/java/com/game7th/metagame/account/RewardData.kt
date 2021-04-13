package com.game7th.metagame.account

import com.game7th.metagame.inventory.dto.FlaskTemplate
import com.game7th.swiped.api.InventoryItemFullInfoDto

sealed class RewardData {

    data class ArtifactRewardData(val item: InventoryItemFullInfoDto) : RewardData()

    data class FlaskRewardData(val flask: FlaskTemplate): RewardData()

    data class CurrencyRewardData(val currency: com.game7th.swiped.api.Currency, val amount: Int): RewardData()
}