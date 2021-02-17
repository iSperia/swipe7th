package com.game7th.swipe.game.actors

import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.game7th.battle.BattleConfig
import com.game7th.battle.SwipeBattle
import com.game7th.battle.event.BattleEvent
import com.game7th.swipe.SwipeGameGdx
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.game.GameScreen
import com.game7th.swipe.game.actors.ui.GameFinishedDialog
import com.game7th.swipe.gestures.SimpleDirectionGestureDetector
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import ktx.actors.onClick
import ktx.async.KtxAsync

class GameView(
        private val context: GdxGameContext,
        private val multiplexer: InputMultiplexer,
        private val config: BattleConfig,
        private val finishCallback: (Boolean) -> Unit
) : Group(), TileDoubleTapCallback {

    lateinit var atlas: TextureAtlas
    lateinit var font: BitmapFont
    val tileField: TileFieldView
    val battleField: BattleView

    lateinit var battle: SwipeBattle
    lateinit var processor: SimpleDirectionGestureDetector

    lateinit var buttonConcede: Label

    val handler = CoroutineExceptionHandler { _, exception ->
        exception.printStackTrace()
    }

    init {
        processor = SimpleDirectionGestureDetector(object : SimpleDirectionGestureDetector.DirectionListener {
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
        })
        multiplexer.addProcessor(0, processor)

        tileField = TileFieldView(context, this).apply {
            setScale(TILE_FIELD_SCALE)
            x = 0f
            y = GameScreen.VP_HEIGHT - 6 * 32 * TILE_FIELD_SCALE -  240
        }
        addActor(tileField)

        battleField = BattleView(context)
        addActor(battleField)

        battle = SwipeBattle(context.balance)
        initializeBattle()
        listenEvents()

        buttonConcede = Label("Concede", Label.LabelStyle(context.font, Color.YELLOW))
        buttonConcede.onClick {
            debugShowBigText(false, "DEFEAT")
        }
        buttonConcede.y = 700f
        addActor(buttonConcede)
    }

    private fun initializeBattle() {
        KtxAsync.launch {
            battle.initialize(config)
        }
    }

    private fun listenEvents() {
        KtxAsync.launch(handler) {
            for (event in battle.events) {
                tileField.processAction(event)
                battleField.processAction(event)
                when (event) {
                    is BattleEvent.VictoryEvent -> {debugShowBigText(true, "VICTORY")}
                    is BattleEvent.DefeatEvent -> {debugShowBigText(false, "DEFEAT")}
                }
            }
        }
    }

    private fun debugShowBigText(victory: Boolean, text: String) {
        tileField.touchable = Touchable.disabled
        multiplexer.removeProcessor(processor)

        val dialog = GameFinishedDialog(context, text) {
            println("Closing scene")
            finishCallback(victory)
        }.apply {
            x = 40f
            y = 220f
            this@GameView.addActor(this)
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