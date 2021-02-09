package com.game7th.battle.tilefield.tile

data class SwipeTile(
        //Type of tile
        val type: TileType,

        //Battle-scoped unique id of this tile
        val id: Int,

        //The size of the stack
        val stackSize: Int,

        val thresholdTier1: Int,
        val thresholdTier2: Int
) {
    fun tier1() = stackSize >= thresholdTier1

    fun tier2() = stackSize >= thresholdTier2
}