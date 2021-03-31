package com.game7th.battle.tilefield.tile

import com.game7th.battle.dto.TileTemplate

data class SwipeTile(
        //Type of tile
        val type: TileTemplate,

        //Battle-scoped unique id of this tile
        val id: Int,

        //The size of the stack
        val stackSize: Int,

        val autoDecrement: Boolean = false,

        val stun: Boolean = false
)