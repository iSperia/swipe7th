package com.game7th.swipe.game.actors

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.game7th.battle.event.BattleEvent
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.game.actors.ui.GameFinishedDialog
import kotlinx.coroutines.launch
import ktx.actors.onClick
import ktx.async.KtxAsync

class GameActor(
        private val context: GdxGameContext,
        private val activateTile: (Int) -> Unit,
        private val finishCallback: (Boolean) -> Unit
) : Group(), TileDoubleTapCallback {

    lateinit var atlas: TextureAtlas
    lateinit var font: BitmapFont
    val tileField: TileFieldView
    val battleField: BattleView

    var buttonConcede: Label

    init {
        tileField = TileFieldView(context, this).apply {
            setScale(TILE_FIELD_SCALE)
            x = 0f
            y = 0f
        }
        addActor(tileField)

        battleField = BattleView(context)
        addActor(battleField)
        battleField.isVisible = false

        buttonConcede = Label("Concede", Label.LabelStyle(context.font, Color.YELLOW))
        buttonConcede.onClick {
            debugShowBigText(false, "DEFEAT")
        }
        buttonConcede.y = 700f
        addActor(buttonConcede)
    }

    private fun debugShowBigText(victory: Boolean, text: String) {
        tileField.touchable = Touchable.disabled

        GameFinishedDialog(context, text) {
            println("Closing scene")
            finishCallback(victory)
        }.apply {
            x = 40f
            y = 220f
            this@GameActor.addActor(this)
        }
    }

    override fun processDoubleTapped(id: Int) {
        KtxAsync.launch {
            activateTile(id)
        }
    }

    suspend fun processAction(event: BattleEvent) {
        battleField.processAction(event)
        tileField.processAction(event)
        when (event) {
            is BattleEvent.VictoryEvent -> debugShowBigText(true, "Victory")
            is BattleEvent.DefeatEvent -> debugShowBigText(false, "Defeat")
        }
    }

    companion object {
        const val TILE_FIELD_SCALE = 480f / (6f * 32f)
    }
}