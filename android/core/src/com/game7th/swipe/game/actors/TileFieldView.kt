package com.game7th.swipe.game.actors

import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.actions.*
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.game7th.battle.event.BattleEvent
import com.game7th.battle.event.TileViewModel
import com.game7th.battle.tilefield.TileFieldEvent
import com.game7th.swipe.game.GdxGameContext
import ktx.actors.alpha

class TileFieldView(
        private val gameContext: GdxGameContext,
        private val tileDoubleTapCallback: TileDoubleTapCallback
) : Group(), TileDoubleTapCallback by tileDoubleTapCallback {

    private val backgroundGroup: Group

    private val tileGroup: Group

    init {
        backgroundGroup = Group().apply {
            for (i in 0..5) {
                for (j in 0..5) {
                    addActor(Image(gameContext.atlas.findRegion(TILE_BG_REGION)).apply {
                        x = j * TILE_SIZE
                        y = TILE_SIZE * (5 - i)
                    })
                }
            }
        }

        tileGroup = Group()

        addActor(backgroundGroup)
        addActor(tileGroup)
    }

    fun processAction(action: BattleEvent) {
        println("Processing action ${action.javaClass.name}")
        when (action) {
            is BattleEvent.CreateTileEvent -> {
                val x = action.position % FIELD_WIDTH
                val y = action.position / FIELD_WIDTH
                val view = TileView(gameContext, action.tile, this)
                view.applyPosition(x, y)
                view.name = "${action.tile.id}"
                tileGroup.addActor(view)

                view.alpha = 0f
                view.setScale(1.5f)
                view.addAction(ParallelAction(
                        AlphaAction().apply {
                            alpha = 1f
                            duration = 0.2f
                        },
                        ScaleToAction().apply {
                            setScale(1f)
                            duration = 0.2f
                        }
                ))
            }
            is BattleEvent.SwipeMotionEvent -> {
                for (event in action.events) {
                    when (event) {
                        is TileFieldEvent.MoveTileEvent -> {
                            val id = event.id
                            val position = event.position
                            animatedMove(id, position)
                        }
                        is TileFieldEvent.MergeTileEvent -> {
                            val tile = event.tile
                            val position = event.position
                            animatedMoveAndDestroy(event.id, position, tile)
                        }
                    }
                }
            }
            is BattleEvent.UpdateTileEvent -> {
                val tile = tileGroup.findActor<TileView>("${action.id}")
                tile.updateFrom(action.tile)
            }
            is BattleEvent.RemoveTileEvent -> {
                val tile = tileGroup.findActor<TileView>("${action.id}")
                tile.addAction(SequenceAction(
                        AlphaAction().apply {
                            alpha = 0f
                            duration = 0.1f
                        },
                        RunnableAction().apply {
                            setRunnable {
                                tile.clearActions()
                                tileGroup.removeActor(tile)
                            }
                        }
                ))
            }
        }
    }

    private fun animatedMove(id: Int, position: Int) {
        val tile = findActor<TileView>("$id")
        tile.addAction(MoveToAction().apply {
            setPosition(32f * (position % FIELD_WIDTH), 32f * (5 - (position / FIELD_WIDTH)))
            duration = MOVE_STEP_LENGTH
        })
    }

    private fun animatedMoveAndDestroy(id: Int, position: Int, updateTile: TileViewModel) {
        val tile = findActor<TileView>("$id")
        tile.addAction(SequenceAction(
                ParallelAction(
                        MoveToAction().apply {
                            setPosition(32f * (position % FIELD_WIDTH), 32f * (5 - (position / FIELD_WIDTH)))
                            duration = MOVE_STEP_LENGTH
                        },
                        AlphaAction().apply {
                            alpha = 0f
                            duration = MOVE_STEP_LENGTH
                        }
                ),
                RunnableAction().apply {
                    setRunnable {
                        findActor<TileView>("${updateTile.id}").updateFrom(updateTile)
                        tile.clearActions()
                        removeActor(tile)
                    }
                }
        ))
    }

    companion object {
        const val TILE_SIZE = 32f
        const val TILE_BG_REGION = "tile_bg_grey"
        const val FIELD_WIDTH = 6
        const val MOVE_STEP_LENGTH = 0.05f
    }
}