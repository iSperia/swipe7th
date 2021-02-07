package com.game7th.battle.tilefield.tile

enum class TileType(
        val skin: String,
        val fraction: Fraction,
        val background: Boolean
) {

    GLADIATOR_STRIKE("skill_tile_holy_strike", Fraction.EMPIRE, true),
    KNIGHT_SHIELD("kn_shield", Fraction.EMPIRE, false)

}
