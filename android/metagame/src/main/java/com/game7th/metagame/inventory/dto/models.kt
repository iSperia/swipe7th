package com.game7th.metagame.inventory.dto

enum class ItemNode {
    BODY, HAND, HEAD, RING, FOOT, BOOK
}

data class InventoryItem(
        val gbFlatBody: Int = 0,
        val gbPercBody: Int = 0,
        val gbFlatSpirit: Int = 0,
        val gbPercSpirit: Int = 0,
        val gbFlatMind: Int = 0,
        val gbPercMind: Int = 0,
        val gbFlatArmor: Int = 0,
        val gbPercArmor: Int = 0,
        val gbFlatHp: Int = 0,
        val gbPercHp: Int = 0,//beware, it is overall hp bonus
        val gbFlatEvasion: Int = 0,
        val gbPercEvasion: Int = 0,
        val gbFlatRegeneration: Int = 0,
        val gbPercRegeneration: Int = 0,
        val gbFlatResist: Int = 0,
        val gbPercResist: Int = 0,
        val gbFlatWisdom: Int = 0,
        val gbPercWisdom: Int = 0,
        val level: Int = 1,
        val name: String,
        val node: ItemNode
)

data class InventoryItemMetadata(
        var template: InventoryItem,
        var minLevel: Int,//constraint minimum level of enemies
        var maxLevel: Int,//constraint maximum level of enemies
        var weight: Int//weight of artifact for drop
)

data class GearConfig(
        val items: List<InventoryItemMetadata>
)

data class InventoryPool(
        val items: MutableList<InventoryItem>
)