package com.game7th.swipe.game.actors

import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.actions.*
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.utils.Sort
import com.game7th.swipe.game.BattleContext
import com.game7th.swiped.api.battle.BattleEvent
import com.game7th.swiped.api.battle.TileFieldEventType
import com.game7th.swiped.api.battle.TileViewModel
import ktx.actors.alpha

class TileFieldView(
        private val context: BattleContext,
        private val w: Float,
        private val h: Float
) : Group() {

    private val tileBackgrounds = Group()
    private val tileForegrounds = Group()

    private val tileGroup = Group()

    val tileSize = (w - 4 * context.scale) / FIELD_WIDTH

    private val polygonSpriteBatch = PolygonSpriteBatch()
    private val tileProgressSpriteBatch = PolygonSpriteBatch()

    init {
        for (i in 0..4) {
            for (j in 0..4) {
                tileBackgrounds.addActor(Image(context.locationAtlas.findRegion(TILE_BG_REGION)).apply {
                    x = (tileSize + context.scale) * j
                    y = (tileSize + context.scale) * (FIELD_WIDTH - 1 - i)
                    width = tileSize
                    height = tileSize
                })

                tileForegrounds.addActor(Image(context.locationAtlas.findRegion(TILE_FG_REGION)).apply {
                    x = (tileSize + context.scale) * j
                    y = (tileSize + context.scale) * (FIELD_WIDTH - 1 - i)
                    width = tileSize
                    height = tileSize
                })
            }
        }

        addActor(tileBackgrounds)
        addActor(tileForegrounds)
        addActor(tileGroup)
    }

    fun processAction(action: BattleEvent) {
        when (action) {
            is BattleEvent.CreateTileEvent -> {
                val tx = action.position % FIELD_WIDTH
                val ty = action.position / FIELD_WIDTH
                val fx = if (action.sourcePosition >= 0) action.sourcePosition % FIELD_WIDTH else tx
                val fy = if (action.sourcePosition >= 0) action.sourcePosition / FIELD_WIDTH else ty

                val tile = action.tile.skin
                val progressEmpty = context.figuresUi[tile]?.findRegion("${tile}_progress_empty")
                val progressFull = context.figuresUi[tile]?.findRegion("${tile}_progress_full")
                val tileBg = context.figuresUi[tile]?.findRegion("${tile}_bg")

                val skin = action.tile.skin
                val view = if (context.staticTiles.contains(skin)) {
                    StaticTileView(context, action.tile, tileSize)
                } else {
                    SpineTileView(context, action.tile, tileSize, polygonSpriteBatch, tileProgressSpriteBatch, progressEmpty, progressFull, tileBg).apply {
                        this.tx = tx
                        this.ty = ty
                    }
                }

                view.applyPosition(fx, fy)
                view.name = "${action.tile.id}"
                tileGroup.addActor(view)
                reorderTiles()

                val animation = SequenceAction()
                val tileAnimation = ParallelAction()
                if (action.sourcePosition >= 0) {
                    tileAnimation.addAction(MoveToAction().apply {
                        setPosition(tileSize * tx , tileSize * (FIELD_WIDTH - 1 - ty))
                        duration = 0.2f
                    })
                }

                view.setScale(1.1f)
                tileAnimation.addAction(ScaleToAction().apply {
                    setScale(1f)
                    duration = 0.2f
                })

                animation.addAction(tileAnimation)
                view.addAction(animation)
            }
            is BattleEvent.SwipeMotionEvent -> {
                for (event in action.events) {
                    when (event.type) {
                        TileFieldEventType.MOVE -> {
                            val id = event.id
                            val position = event.position
                            animatedMove(id, position)
                        }
                        TileFieldEventType.MERGE -> {
                            val position = event.position
                            animatedMoveAndDestroy(event.id, position, event.tile!!)
                        }
                        TileFieldEventType.DELETE -> {
                            val tile = tileGroup.findActor<AbstractTileView>("${event.id}")
                            removeTile(tile)
                        }
                        TileFieldEventType.UPDATE -> {
                            val tile = tileGroup.findActor<AbstractTileView>("${event.id}")
                            event.tile?.let { tileViewModel ->
                                tile.addAction(SequenceAction(
                                        RunnableAction().apply { setRunnable { tile.updateFrom(tileViewModel)
                                        }}
                                ))
                            }
                        }
                    }
                }
            }
            is BattleEvent.UpdateTileEvent -> {
                val tile = tileGroup.findActor<SpineTileView>("${action.id}")
                tile.addAction(SequenceAction(
                        RunnableAction().apply { setRunnable { tile.updateFrom(action.tile)
                        }}
                ))
            }
            is BattleEvent.RemoveTileEvent -> {
                val tile = tileGroup.findActor<SpineTileView>("${action.id}")
                removeTile(tile)
            }
            is BattleEvent.ShowTileEffect -> {
//                val x = action.position % 5
//                val y = action.position / 5
//                val effect = Image(context.battleAtlas.findRegion(action.effect)).apply {
//                    width = tileSize
//                    height = tileSize
//                }
//                effect.applyPosition(x, y)
//                tileEffectGroup.addActor(effect)
//                effect.setScale(1.2f)
//                effect.alpha = 0f
//                effect.addAction(SequenceAction(
//                        ParallelAction(
//                                ScaleToAction().apply { duration = 0.3f; setScale(1f) },
//                                AlphaAction().apply { alpha = 1f; duration = 0.1f }
//                        ),
//                        RunnableAction().apply { setRunnable { effect.remove() } }
//                ))
            }
        }
    }

    private fun removeTile(tile: AbstractTileView) {
        tile.removed = true
        tile?.addAction(SequenceAction(
                AlphaAction().apply {
                    alpha = 0f
                    duration = 0.1f
                },
                RunnableAction().apply {
                    setRunnable {
                        tile.clearActions()
                        tile.remove()
                    }
                }
        ))
    }

    private fun reorderTiles() {
        tileGroup.children.sortedBy { if (it is LayerProvider) it.getLayer() else 1000 }.withIndex().forEach {
            it.value.zIndex = it.index
        }
    }

    private fun animatedMove(id: Int, position: Int) {
        findActor<AbstractTileView>("$id")?.let { tile ->
            tile.tx = position % FIELD_WIDTH
            tile.ty = position / FIELD_WIDTH
            tile.durationActionQueue.add(MoveToAction().apply {
                setPosition(tileSize * (position % FIELD_WIDTH), tileSize * (FIELD_WIDTH - 1 - (position / FIELD_WIDTH)))
                duration = MOVE_STEP_LENGTH
            })
        }
    }

    private fun animatedMoveAndDestroy(id: Int, position: Int, updateTile: TileViewModel) {
        findActor<SpineTileView>("$id")?.let { tile ->
            tile.tx = position % FIELD_WIDTH
            tile.ty = position / FIELD_WIDTH
            tile.removed = true
            tile.durationActionQueue.add(SequenceAction(
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
                            val tileToUpdate = findActor<SpineTileView>("${updateTile.id}")
                            tileToUpdate?.updateFrom(updateTile)
                            tile.clearActions()
                            tile.remove()
                        }
                    }
            ))
        }
    }

    fun finalizeActions() {
        tileGroup.children.forEach { tileActor ->
            (tileActor as? SpineTileView)?.let { tileView ->
                tileView.applyPosition(tileView.tx, tileView.ty)
                tileView.setScale(1f)
                tileView.alpha = 1f
                tileView.clearActions()
                if (tileView.removed) tileView.remove()
            }
        }
    }

    private fun Actor.applyPosition(x: Int, y: Int) {
        this.x = (context.scale + tileSize) * x
        this.y = (context.scale + tileSize) * (FIELD_WIDTH - 1 - y)
    }

    companion object {
        const val TILE_BG_REGION = "tile_bg"
        const val TILE_FG_REGION = "tile_fg"
        const val FIELD_WIDTH = 5
        const val MOVE_STEP_LENGTH = 0.025f
    }
}