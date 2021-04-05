package com.game7th.swipe.game.actors

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.actions.*
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.game7th.battle.dto.BattleEvent
import com.game7th.battle.dto.TileViewModel
import com.game7th.battle.tilefield.TileFieldEvent
import com.game7th.swipe.GdxGameContext
import ktx.actors.alpha
import kotlin.random.Random

class TileFieldView(
        private val gameContext: GdxGameContext,
        private val w: Float,
        private val h: Float
) : Group() {

    private val backgroundGroup: Group

    private val tileGroup: Group
    private val tileEffectGroup: Group

    val tileSize = w / FIELD_WIDTH

    private val shapeRenderer = ShapeRenderer()

    init {
        backgroundGroup = Group().apply {
            for (i in 0..4) {
                for (j in 0..4) {
                    addActor(Image(gameContext.battleAtlas.findRegion(TILE_BG_REGION)).apply {
                        val ax = tileSize * j
                        val ay = tileSize * (FIELD_WIDTH - 1 - i)

                        x = tileSize * j - tileSize / 2 + tileSize * Random.nextFloat()
                        y = tileSize * (FIELD_WIDTH - 1 - i) - tileSize / 2 + tileSize * Random.nextFloat()
                        width = tileSize
                        height = tileSize
                        setScale(2f)
                        addAction(ParallelAction(
                                MoveToAction().apply { setPosition(ax, ay); duration = 1.8f },
                                ScaleToAction().apply { setScale(1f); duration = 1.6f }
                        ))
                    })
                }
            }
        }

        tileGroup = Group()
        tileEffectGroup = Group()

        addActor(backgroundGroup)
        addActor(tileGroup)
        addActor(tileEffectGroup)
    }

    fun processAction(action: BattleEvent) {
        when (action) {
            is BattleEvent.CreateTileEvent -> {
                val tx = action.position % FIELD_WIDTH
                val ty = action.position / FIELD_WIDTH
                val fx = if (action.sourcePosition >= 0) action.sourcePosition % FIELD_WIDTH else tx
                val fy = if (action.sourcePosition >= 0) action.sourcePosition / FIELD_WIDTH else ty

                val view = TileView(gameContext, action.tile, tileSize, shapeRenderer).apply {
                    this.tx = tx
                    this.ty = ty
                }
                view.applyPosition(fx, fy)
                view.name = "${action.tile.id}"
                tileGroup.addActor(view)

                val animation = SequenceAction()
                val tileAnimation = ParallelAction()
                if (action.sourcePosition >= 0) {
                    tileAnimation.addAction(MoveToAction().apply {
                        setPosition(tileSize * tx , tileSize * (FIELD_WIDTH - 1 - ty))
                        duration = 0.2f
                    })
                }

                view.alpha = 0f
                view.setScale(1.5f)
                tileAnimation.addAction(AlphaAction().apply {
                    alpha = 1f
                    duration = 0.2f
                })
                tileAnimation.addAction(ScaleToAction().apply {
                    setScale(1f)
                    duration = 0.2f
                })

                animation.addAction(tileAnimation)
                view.addAction(animation)
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
                tile.addAction(SequenceAction(
                        RunnableAction().apply { setRunnable { tile.updateFrom(action.tile)
                        }}
                ))
            }
            is BattleEvent.RemoveTileEvent -> {
                val tile = tileGroup.findActor<TileView>("${action.id}")
                tile.removed = true
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
            is BattleEvent.ShowTileEffect -> {
                val x = action.position % 5
                val y = action.position / 5
                val effect = Image(gameContext.battleAtlas.findRegion(action.effect)).apply {
                    width = tileSize
                    height = tileSize
                }
                effect.applyPosition(x, y)
                tileEffectGroup.addActor(effect)
                effect.setScale(1.2f)
                effect.alpha = 0f
                effect.addAction(SequenceAction(
                        ParallelAction(
                                ScaleToAction().apply { duration = 0.3f; setScale(1f) },
                                AlphaAction().apply { alpha = 1f; duration = 0.1f }
                        ),
                        RunnableAction().apply { setRunnable { effect.remove() } }
                ))
            }
        }
    }

    private fun animatedMove(id: Int, position: Int) {
        val tile = findActor<TileView>("$id")
        tile.tx = position % FIELD_WIDTH
        tile.ty = position / FIELD_WIDTH
        tile?.addAction(
                SequenceAction(
                        MoveToAction().apply {
                            setPosition(tileSize * (position % FIELD_WIDTH), tileSize * (FIELD_WIDTH - 1 - (position / FIELD_WIDTH)))
                            duration = MOVE_STEP_LENGTH
                        }
                ))
    }

    private fun animatedMoveAndDestroy(id: Int, position: Int, updateTile: TileViewModel) {
        val tile = findActor<TileView>("$id")
        findActor<TileView>("${updateTile.id}")?.addAction(SequenceAction(
                RunnableAction().apply {
                    setRunnable {
                        findActor<TileView>("${updateTile.id}").updateFrom(updateTile)
                    }
                }
        ))
        tile.tx = position % FIELD_WIDTH
        tile.ty = position / FIELD_WIDTH
        tile.removed = true
        tile.addAction(SequenceAction(
                ParallelAction(
                        RunnableAction().apply { setRunnable { tile.setScale(1.3f) } },
                        MoveToAction().apply {
                            setPosition(tileSize * (position % FIELD_WIDTH), tileSize * (FIELD_WIDTH - 1 - (position / FIELD_WIDTH)))
                            duration = MOVE_STEP_LENGTH
                        },
                        AlphaAction().apply {
                            alpha = 0f
                            duration = MOVE_STEP_LENGTH
                        },
                        ScaleToAction().apply {
                            setScale(0.7f)
                            duration = MOVE_STEP_LENGTH
                        }
                ),
                RunnableAction().apply {
                    setRunnable {
                        val tileToUpdate = findActor<TileView>("${updateTile.id}")
                        tileToUpdate?.updateFrom(updateTile)
                        tile.clearActions()
                        tile.remove()
                    }
                }
        ))
    }

    fun finalizeActions() {
//        tileGroup.children.forEach { tileActor ->
//            (tileActor as? TileView)?.let { tileView ->
//                tileView.applyPosition(tileView.tx, tileView.ty)
//                tileView.setScale(1f)
//                tileView.alpha = 1f
//                tileView.clearActions()
//                if (tileView.removed) tileView.remove()
//            }
//        }
//        moveShift = 0
    }

    private fun Actor.applyPosition(x: Int, y: Int) {
        this.x = tileSize * x
        this.y = tileSize * (FIELD_WIDTH - 1 - y)
    }

    fun calcX(position: Int): Float = tileSize * (position % 5)
    fun calcY(position: Int): Float = tileSize * (FIELD_WIDTH - 1 - (position / FIELD_WIDTH))

    companion object {
        const val TILE_BG_REGION = "tile_bg_grey"
        const val FIELD_WIDTH = 5
        const val MOVE_STEP_LENGTH = 0.1f
    }
}