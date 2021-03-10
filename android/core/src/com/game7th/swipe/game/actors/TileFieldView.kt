package com.game7th.swipe.game.actors

import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.actions.*
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.game7th.battle.event.BattleEvent
import com.game7th.battle.event.TileViewModel
import com.game7th.battle.tilefield.TileFieldEvent
import com.game7th.swipe.GdxGameContext
import kotlinx.coroutines.delay
import ktx.actors.alpha

class TileFieldView(
        private val gameContext: GdxGameContext
) : Group() {

    private val backgroundGroup: Group

    private val tileGroup: Group

    init {
        backgroundGroup = Group().apply {
            for (i in 0..4) {
                for (j in 0..4) {
                    addActor(Image(gameContext.atlas.findRegion(TILE_BG_REGION)).apply {
                        x = TILE_SIZE / 2 + TILE_SIZE * j
                        y = TILE_SIZE * (FIELD_WIDTH - 1 - i)
                        width = TILE_SIZE
                        height = TILE_SIZE
                    })
                }
            }
        }

        tileGroup = Group()

        addActor(backgroundGroup)
        addActor(tileGroup)
    }

    suspend fun processAction(action: BattleEvent) {
        when (action) {
            is BattleEvent.CreateTileEvent -> {
                val tx = action.position % FIELD_WIDTH
                val ty = action.position / FIELD_WIDTH
                val fx = if (action.sourcePosition >= 0) action.sourcePosition % FIELD_WIDTH else tx
                val fy = if (action.sourcePosition >= 0) action.sourcePosition / FIELD_WIDTH else ty

                val view = TileView(gameContext, action.tile)
                view.applyPosition(fx, fy)
                view.name = "${action.tile.id}"
                tileGroup.addActor(view)

                if (action.sourcePosition >= 0) {
                    view.addAction(MoveToAction().apply {
                        setPosition(36f * tx + 18f, 36f * (FIELD_WIDTH - 1 - ty))
                        duration = 0.2f
                    })
                }

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
                delay(50L)
            }
            is BattleEvent.UpdateTileEvent -> {
                val tile = tileGroup.findActor<TileView>("${action.id}")
                tile.updateFrom(action.tile)
            }
            is BattleEvent.RemoveTileEvent -> {
                val tile = tileGroup.findActor<TileView>("${action.id}")
                tile?.addAction(SequenceAction(
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
        tile?.addAction(MoveToAction().apply {
            setPosition(18f + 36f * (position % FIELD_WIDTH), 36f * (FIELD_WIDTH - 1 - (position / FIELD_WIDTH)))
            duration = MOVE_STEP_LENGTH
        })
    }

    private fun animatedMoveAndDestroy(id: Int, position: Int, updateTile: TileViewModel) {
        val tile = findActor<TileView>("$id")
        findActor<TileView>("${updateTile.id}")?.addAction(SequenceAction(
                DelayAction(MOVE_STEP_LENGTH),
                RunnableAction().apply {
                    setRunnable {
                        findActor<TileView>("${updateTile.id}").updateFrom(updateTile)
                    }
                }
        ))
        tile?.addAction(SequenceAction(
                ParallelAction(
                        MoveToAction().apply {
                            setPosition(18f + 36f * (position % FIELD_WIDTH), 36f * (FIELD_WIDTH - 1 - (position / FIELD_WIDTH)))
                            duration = MOVE_STEP_LENGTH
                        },
                        AlphaAction().apply {
                            alpha = 0f
                            duration = MOVE_STEP_LENGTH
                        },
                        ScaleToAction().apply {
                            setScale(1.3f)
                            duration = MOVE_STEP_LENGTH
                        }
                ),
                RunnableAction().apply {
                    setRunnable {
                        tile.clearActions()
                        tile.remove()
                    }
                }
        ))
    }

    companion object {
        const val TILE_SIZE = 36f
        const val TILE_BG_REGION = "tile_bg_grey"
        const val FIELD_WIDTH = 5
        const val MOVE_STEP_LENGTH = 0.05f
    }
}