package com.game7th.metagame.inventory.dto
import com.game7th.swiped.api.InventoryItemFullInfoDto

data class GearConfig(
        val flasks: List<FlaskDto>
)

data class InventoryPool(
        val flasks: MutableList<FlaskStackDto>
)

data class FlaskTemplate(
        val name: String,
        val fbFlatHeal: Int,
        val fbRemoveStun: Int,
        val fbSummonSlime: Int = 0
)

data class FlaskStackDto(
        val template: FlaskTemplate,
        var amount: Int
)

data class FlaskDto(
        val template: FlaskTemplate,
        val dropWeight: Int,
        val gold: Int,
        val rarity: Int
)