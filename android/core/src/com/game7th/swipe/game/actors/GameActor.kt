package com.game7th.swipe.game.actors

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.game7th.battle.event.BattleEvent
import com.game7th.metagame.account.PersonageExperienceResult
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.game.actors.ui.GameFinishedDialog
import com.game7th.swipe.game.battle.hud.HudGroup
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
    val hudGroup: HudGroup

    var buttonConcede: Label

    var buttonCombo: Label

    init {
        tileField = TileFieldView(context, this).apply {
            setScale(TILE_FIELD_SCALE)
            x = 0f
            y = 0f
        }
        addActor(tileField)

        hudGroup = HudGroup(context).apply {
            y = TILE_FIELD_SCALE * 5.5f * 36f
        }
        addActor(hudGroup)

        buttonConcede = Label("Concede", Label.LabelStyle(context.font, Color.YELLOW)).apply {
            x = 300f
            y = 700f
        }
        buttonConcede.onClick {
            debugShowBigText(false, "DEFEAT")
        }
        addActor(buttonConcede)

        buttonCombo = Label("COMBO", Label.LabelStyle(context.font, Color.WHITE)).apply {
            y = 700f
            x = 10f
            setFontScale(1.2f)
            isVisible = false
        }
        addActor(buttonCombo)
    }

    private fun debugShowBigText(victory: Boolean, text: String) {
        tileField.touchable = Touchable.disabled

        GameFinishedDialog(context, text, null) {
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

    fun showDefeat() {
        debugShowBigText(false, "Defeat")
    }

    fun showVictory(expResult: PersonageExperienceResult) {
        GameFinishedDialog(context, "Victory", expResult) {
            finishCallback(true)
        }.apply {
            x = 40f
            y = 220f
            this@GameActor.addActor(this)
        }
    }

    suspend fun processAction(event: BattleEvent) {
//        battleField.processAction(event)
        tileField.processAction(event)
        when (event) {
            is BattleEvent.CreatePersonageEvent -> {
                hudGroup.showHud(event.position, event.personage)
            }
            is BattleEvent.PersonageUpdateEvent -> {
                hudGroup.updateHud(event.personage)
            }
            is BattleEvent.ComboUpdateEvent -> {
                buttonCombo.isVisible = event.combo > 0
                buttonCombo.setText("COMBO X${event.combo}")
            }
        }
    }

    companion object {
        const val TILE_FIELD_SCALE = 480f / (6f * 36f)
    }
}