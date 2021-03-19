package com.game7th.swipe.game.actors

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.ParticleEffect
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.ScaleToAction
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.game7th.battle.dto.BattleEvent
import com.game7th.metagame.account.dto.PersonageExperienceResult
import com.game7th.metagame.account.RewardData
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.game.actors.ui.GameFinishedDialog
import ktx.actors.onClick
import ktx.actors.repeatForever
import kotlin.math.min

class GameActor(
        private val context: GdxGameContext,
        private val rewardCallback: () -> List<RewardData>,
        private val finishCallback: (Boolean) -> Unit
) : Group() {

    lateinit var atlas: TextureAtlas
    val tileField: TileFieldView

    val buttonConcede: Label
    val labelCombo: Label
    val labelComboWrapper: Group
    val tileFieldZoneBorder: Image
    val tileFieldBorder: Image
    val bottomSheetBg: Image
    val comboParticles: ParticleEffect

    private var combo = 0

    val tileFieldAreaHeight = min(Gdx.graphics.width.toFloat(), Gdx.graphics.height - Gdx.graphics.width / 1.25f - 48f * context.scale) - 32f

    init {
        Image(context.uiAtlas.findRegion("ui_dialog")).apply {
            x = 0f
            y = 48f * context.scale
            width = Gdx.graphics.width.toFloat()
            height = Gdx.graphics.height - Gdx.graphics.width * 0.8f - 48f * context.scale
            color = Color.GRAY
        }.let { addActor(it) }
        tileFieldZoneBorder = Image(context.uiAtlas.createPatch("bg_brass")).apply {
            x = 0f
            y = 48f * context.scale
            width = Gdx.graphics.width.toFloat()
            height = Gdx.graphics.height - Gdx.graphics.width * 0.8f - 48f * context.scale
        }
        addActor(tileFieldZoneBorder)

        tileField = TileFieldView(context, tileFieldAreaHeight, tileFieldAreaHeight).apply {
            x = (Gdx.graphics.width - tileFieldAreaHeight) / 2f
            y = 48f * context.scale + (Gdx.graphics.height - Gdx.graphics.width * 0.8f - 48f * context.scale - tileFieldAreaHeight) / 2
        }
        addActor(tileField)
        tileFieldBorder = Image(context.uiAtlas.createPatch("bg_brass")).apply {
            x = tileField.x - 8f
            y = tileField.y - 8f
            width = tileFieldAreaHeight + 16f
            height = tileFieldAreaHeight + 16f
        }
        addActor(tileFieldBorder)

        bottomSheetBg = Image(context.uiAtlas.findRegion("ui_dialog")).apply {
            x = 0f
            y = 0f
            width = Gdx.graphics.width.toFloat()
            height = 48f * context.scale
        }
        addActor(bottomSheetBg)

        buttonConcede = Label("Concede", Label.LabelStyle(context.font, Color.BLACK)).apply {
            setFontScale(22f * context.scale / 36f)
            width = 100f * context.scale
            x = Gdx.graphics.width - width - 10f * context.scale
            y = 30f
            setAlignment(Align.center)
        }
        buttonConcede.onClick {
            showDefeat()
        }
        addActor(buttonConcede)

        labelCombo = Label("COMBO", Label.LabelStyle(context.font2, Color.SCARLET)).apply {
            x = -Gdx.graphics.width / 2f
            y = -20f * context.scale
            width = Gdx.graphics.width.toFloat()
            height = 40f * context.scale
            setFontScale(60f * context.scale / 36f)
            setAlignment(Align.center)
            isVisible = false
        }
        labelComboWrapper = Group().apply {
            y = Gdx.graphics.height - 40f * context.scale
            x = Gdx.graphics.width / 2f
        }
        labelComboWrapper.addActor(labelCombo)
        addActor(labelComboWrapper)

        comboParticles = ParticleEffect()
        comboParticles.load(Gdx.files.internal("particles_0"), context.battleAtlas)
        comboParticles.setPosition(labelComboWrapper.x, labelComboWrapper.y + labelCombo.y - 10f * context.scale)
        comboParticles.scaleEffect(3f)
    }

    internal fun showDefeat() {
        tileField.touchable = Touchable.disabled

//        Gdx.audio.newMusic(Gdx.files.internal("sounds/defeat.ogg")).let { it.play() }
        GameFinishedDialog(context, "Defeat", emptyList(), emptyList()) {
            finishCallback(false)
        }.apply {
            x = 40f
            y = 220f
            this@GameActor.addActor(this)
        }
    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        if (combo > 1) {
            comboParticles.draw(batch, Gdx.graphics.deltaTime * 0.1f * min(combo, 8))
        }
        super.draw(batch, parentAlpha)
    }

    fun showVictory(expResult: List<PersonageExperienceResult>) {
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

    fun processAction(event: BattleEvent) {
        tileField.processAction(event)
        when (event) {
            is BattleEvent.ComboUpdateEvent -> {
                combo = event.combo
                labelCombo.isVisible = event.combo > 0
                labelCombo.setText("COMBO X${event.combo}")
                labelComboWrapper.clearActions()
                if (event.combo > 0) {
                    labelComboWrapper.addAction(SequenceAction(
                            ScaleToAction().apply { setScale(1.1f + 0.03f * min(8, event.combo)); duration = 2f / min(8, event.combo) },
                            ScaleToAction().apply { setScale(0.9f - 0.01f * min(8, event.combo)); duration = 2f / min(8, event.combo) }
                    ).repeatForever())
                    comboParticles.reset()
                    comboParticles.scaleEffect(3f + min(8, combo) * 0.2f)
                }
            }
        }
    }
}