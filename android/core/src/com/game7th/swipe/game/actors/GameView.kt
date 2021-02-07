package com.game7th.swipe.game.actors

import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.Group
import com.game7th.battle.SwipeBattle
import com.game7th.battle.event.BattleEvent
import com.game7th.swipe.SwipeGameGdx
import com.game7th.swipe.game.GdxGameContext
import com.game7th.swipe.gestures.SimpleDirectionGestureDetector
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ktx.async.KtxAsync

class GameView(
        private val context: GdxGameContext,
        private val multiplexer: InputMultiplexer
) : Group(), TileDoubleTapCallback {

    lateinit var atlas: TextureAtlas
    lateinit var font: BitmapFont
    val tileField: TileFieldView
    val battleField: BattleView

    lateinit var battle: SwipeBattle

    val handler = CoroutineExceptionHandler { _, exception ->
        exception.printStackTrace()
    }

    init {
        multiplexer.addProcessor(0, SimpleDirectionGestureDetector(object : SimpleDirectionGestureDetector.DirectionListener {
            override fun onLeft() {
                KtxAsync.launch {
                    battle.processSwipe(-1, 0)
                }
            }

            override fun onRight() {
                KtxAsync.launch {
                    battle.processSwipe(1, 0)
                }
            }

            override fun onUp() {
                KtxAsync.launch {
                    battle.processSwipe(0, -1)
                }
            }

            override fun onDown() {
                KtxAsync.launch {
                    battle.processSwipe(0, 1)
                }
            }
        }))

        tileField = TileFieldView(context, this).apply {
            setScale(TILE_FIELD_SCALE)
            x = 0f
            y = SwipeGameGdx.VP_HEIGHT - 6 * 32 * TILE_FIELD_SCALE -  240
        }
        addActor(tileField)

        battleField = BattleView(context)
        addActor(battleField)

        battle = SwipeBattle(context.balance)
        initializeBattle()
        listenEvents()
    }

    private fun initializeBattle() {
        KtxAsync.launch {
            battle.initialize()
        }
    }

    private fun listenEvents() {
        KtxAsync.launch(handler) {
            for (event in battle.events) {
                when (event) {
                    is BattleEvent.CreateTileEvent -> tileField.processAction(event)
                    is BattleEvent.UpdateTileEvent -> tileField.processAction(event)
                    is BattleEvent.RemoveTileEvent -> tileField.processAction(event)
                    is BattleEvent.SwipeMotionEvent -> {
                        tileField.processAction(event)
                        delay(50L)
                    }
                    is BattleEvent.CreatePersonageEvent -> battleField.processAction(event)
                    is BattleEvent.PersonageAttackEvent -> battleField.processAction(event)
                    is BattleEvent.PersonageDamageEvent -> battleField.processAction(event)
                }
            }
        }
    }

    override fun processDoubleTapped(id: Int) {
        KtxAsync.launch {
            battle.attemptActivateTile(id)
        }
    }

    companion object {
        const val TILE_FIELD_SCALE = 480f / (6f * 32f)
    }
}