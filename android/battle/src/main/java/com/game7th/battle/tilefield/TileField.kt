package com.game7th.battle.tilefield

import com.game7th.battle.tilefield.tile.SwipeTile
import com.game7th.battle.toViewModel
import java.util.concurrent.ConcurrentHashMap

class TileField(private val merger: TileFieldMerger) {

    //all the tiles by their position
    var tiles = ConcurrentHashMap<Int, SwipeTile>()

    var tileId = 0

    suspend fun attemptSwipe(dx: Int, dy: Int, reality: Boolean): List<TileFieldEvent> {
        val result = mutableListOf<TileFieldEvent>()

        val orderedPositions = tiles.keys.toList().sortedWith(Comparator { p1, p2 ->
            val x1 = p1 % WIDTH
            val y1 = p1 / WIDTH
            val x2 = p2 % WIDTH
            val y2 = p2 / WIDTH

            when {
                dx > 0 -> x2.compareTo(x1)
                dx < 0 -> x1.compareTo(x2)
                dy > 0 -> y2.compareTo(y1)
                dy < 0 -> y1.compareTo(y2)
                else -> 0
            }
        })

        val freshGeneration = mutableMapOf<Int, SwipeTile>()

        orderedPositions.forEach { position ->
            val x = position % WIDTH
            val y = position / WIDTH
            val nx = x + dx
            val ny = y + dy
            val tile = tiles[position]!!

            val newPosition = nx + ny * WIDTH

            if (nx >= 0 && ny >= 0 && nx < WIDTH && ny < HEIGHT) {
                if (!freshGeneration.containsKey(newPosition)) {
                    if (!tile.stun) {
                        //we are fine to move
                        result.add(TileFieldEvent.MoveTileEvent(tile.id, newPosition))
                        freshGeneration[newPosition] = tile
                    } else {
                        freshGeneration[position] = tile
                    }
                } else {
                    //we are maybe merge subjects
                    val mergeResult = merger.merge(tile, freshGeneration[newPosition]!!)
                    if (mergeResult != null) {
                        //we are merging
                        freshGeneration[newPosition] = mergeResult
                        result.add(TileFieldEvent.MergeTileEvent(tile.id, mergeResult.toViewModel(), newPosition))
                    } else {
                        //stand still
                        freshGeneration[position] = tile
                    }
                }
            } else {
                //stand still we are on edges
                freshGeneration[position] = tile
            }
        }

        if (reality) {
            tiles.clear()
            tiles.putAll(freshGeneration)
        }

        return result
    }

    fun calculateFreePosition(): Int? {
        return (0 until WIDTH * HEIGHT).filter { !tiles.containsKey(it) }.let {
            if (it.isEmpty()) null else it.random()
        }
    }

    suspend fun updateById(id: Int, tile: SwipeTile) {
        tiles.forEach {
            if (it.value.id == tile.id) {
                tiles[it.key] = tile
                return
            }
        }
    }

    fun removeById(id: Int) {
        val tile = tiles.filter { it.value.id == id }.keys.firstOrNull()
        tile?.let { tiles.remove(it) }
    }

    fun newTileId(): Int {
        return ++tileId
    }

    companion object {
        const val WIDTH = 5
        const val HEIGHT = 5
    }
}