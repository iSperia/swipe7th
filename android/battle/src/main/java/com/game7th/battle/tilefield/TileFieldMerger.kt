package com.game7th.battle.tilefield

import com.game7th.battle.tilefield.tile.SwipeTile

interface TileFieldMerger {

    suspend fun merge(tile: SwipeTile, swipeTile: SwipeTile): SwipeTile?
}