package com.game7th.swipe.game.actors

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.game7th.battle.event.BattleEvent
import com.game7th.metagame.account.PersonageExperienceResult
import com.game7th.metagame.account.RewardData
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.game.actors.ui.GameFinishedDialog
import ktx.actors.onClick
import kotlin.math.min

class GameActor(
        private val context: GdxGameContext,
        private val rewardCallback: () -> List<RewardData>,
        private val finishCallback: (Boolean) -> Unit
) : Group() {

    lateinit var atlas: TextureAtlas
    lateinit var font: BitmapFont
    val tileField: TileFieldView

    val buttonConcede: Label
    val buttonCombo: Label
    val tileFieldZoneBorder: Image
    val tileFieldBorder: Image
    val bottomSheetBg: Image

    val tileFieldAreaHeight = min(Gdx.graphics.width.toFloat(), Gdx.graphics.height - Gdx.graphics.width / 1.25f - 48f * context.scale) - 32f

    init {
        Image(context.uiAtlas.findRegion("ui_dialog")).apply {
            x = 0f
            y = 48f * context.scale
            width = Gdx.graphics.width.toFloat()
            height = tileFieldAreaHeight + 32f
            color = Color.GRAY
        }.let { addActor(it) }
        tileFieldZoneBorder = Image(context.uiAtlas.createPatch("bg_brass")).apply {
            x = 0f
            y = 48f * context.scale
            width = Gdx.graphics.width.toFloat()
            height = tileFieldAreaHeight + 32f
        }
        addActor(tileFieldZoneBorder)

        tileField = TileFieldView(context, tileFieldAreaHeight, tileFieldAreaHeight).apply {
            x = (Gdx.graphics.width - tileFieldAreaHeight) / 2f
            y = 48f * context.scale + 16f
        }
        addActor(tileField)
        tileFieldBorder = Image(context.uiAtlas.createPatch("bg_brass")).apply {
            x = tileField.x - 8f
            y = tileField.y - 8f
            width = tileFieldAreaHeight + 16f
            height = tileFieldAreaHeight + 16f
        }
        addActor(tileFieldBorder)

        buttonConcede = Label("Concede", Label.LabelStyle(context.font, Color.YELLOW)).apply {
            x = 300f
            y = 700f
        }
        buttonConcede.onClick {
            showDefeat()
        }
        addActor(buttonConcede)

        buttonCombo = Label("COMBO", Label.LabelStyle(context.font, Color.WHITE)).apply {
            y = 700f
            x = 10f
            setFontScale(1.2f)
            isVisible = false
        }
        addActor(buttonCombo)

        bottomSheetBg = Image(context.uiAtlas.findRegion("ui_dialog")).apply {
            x = 0f
            y = 0f
            width = Gdx.graphics.width.toFloat()
            height = 48f * context.scale
        }
        addActor(bottomSheetBg)
    }

    internal fun showDefeat() {
        tileField.touchable = Touchable.disabled

//        Gdx.audio.newMusic(Gdx.files.internal("sounds/defeat.ogg")).let { it.play() }
        GameFinishedDialog(context, "Defeat", null, emptyList()) {
            finishCallback(false)
        }.apply {
            x = 40f
            y = 220f
            this@GameActor.addActor(this)
        }
    }

    fun showVictory(expResult: PersonageExperienceResult) {
//        Gdx.audio.newMusic(Gdx.files.internal("sounds/victory.ogg")).let { it.play() }
        val rewards = rewardCallback()
        GameFinishedDialog(context, "Victory", expResult, rewards) {
            finishCallback(true)
        }.apply {
            x = 40f
            y = 220f
            this@GameActor.addActor(this)
        }
    }

    suspend fun processAction(event: BattleEvent) {
        tileField.processAction(event)
        when (event) {
            is BattleEvent.ComboUpdateEvent -> {
                buttonCombo.isVisible = event.combo > 0
                buttonCombo.setText("COMBO X${event.combo}")
            }
        }
    }
}