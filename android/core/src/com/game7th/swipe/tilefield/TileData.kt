package com.game7th.swipe.tilefield

enum class TileFraction(private val suffix: String) {
    EMPIRE("empire"),
    CLANS("clans"),
    ACADEMY("academy"),
    DOMAIN("domain"),
    TRIBES("tribes"),
    ALLIANCE("alliance");

    val tileForegroundTexture = "tile_fg_$suffix"
}

enum class TileStage(private val index: Int) {
    INITIAL(0),
    USABLE(1),
    ULTIMATE(2)
}

sealed class TileData(
        val id: Int,
        val position: Int
) {

    class SkillTileData(
            id: Int,
            position: Int,
            val fraction: TileFraction,
            val stage: TileStage,
            val skin: String
    ): TileData(id, position) {
        val skinTexture = "skill_tile_$skin"
    }
}
