package com.game7th.battle.tilefield

import com.game7th.battle.event.TileViewModel

sealed class TileFieldEvent {

    data class MoveTileEvent(val id: Int, val position: Int) : TileFieldEvent()
    data class MergeTileEvent(val id: Int, val tile: TileViewModel, val position: Int) : TileFieldEvent()

}