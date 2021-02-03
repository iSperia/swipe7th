package com.game7th.swipe.tilefield

sealed class TilefieldAction {

    data class CreateTile(
            val position: Int,
            val tileData: Any
    ): TilefieldAction()

    data class RemoveTile(
        val id: Int
    ): TilefieldAction()

    data class MoveTiles(
        val ids: List<Int>,
        val dx: Int,
        val dy: Int
    ): TilefieldAction()
}