package com.game7th.battle.tilefield.tile

data class SwipeTile(
        //Type of tile
        val type: TileType,

        //Battle-scoped unique id of this tile
        val id: Int,

        //The size of the stack
        val stackSize: Int,

        //The stage of tile if applicable
        val stage: TileStage
)