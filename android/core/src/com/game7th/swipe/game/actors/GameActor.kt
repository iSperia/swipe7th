package com.game7th.swipe.game.actors

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.ParticleEffect
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.DelayAction
import com.badlogic.gdx.scenes.scene2d.actions.MoveByAction
import com.badlogic.gdx.scenes.scene2d.actions.ScaleToAction
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.game7th.battle.dto.BattleEvent
import com.game7th.metagame.account.dto.PersonageExperienceResult
import com.game7th.metagame.account.RewardData
import com.game7th.metagame.inventory.GearService
import com.game7th.metagame.inventory.dto.FlaskStackDto
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.alchemy.AlchemyPanel
import com.game7th.swipe.alchemy.AlchemyPanelMode
import com.game7th.swipe.game.GameScreen
import com.game7th.swipe.game.actors.ui.BattleFinishedDialog
import com.game7th.swipe.util.IconTextButton
import com.game7th.swipe.util.animateHideToBottom
import com.game7th.swipe.util.animateShowFromBottom
import ktx.actors.onClick
import ktx.actors.repeatForever
import kotlin.math.min

class GameActor(
        private val context: GdxGameContext,
        private val gearService: GearService,
        private val screen: GameScreen,
        private val usePotionCallback: (FlaskStackDto) -> Unit,
        private val rewardCallback: () -> List<RewardData>,
        private val finishCallback: (Boolean) -> Unit
) : Group() {

    lateinit var atlas: TextureAtlas
    val tileField: TileFieldView

    val buttonConcede: Label
    val buttonPotions = IconTextButton(context, "icon_alch", "Potions", this::switchPotions)
    val labelCombo: Label
    val labelComboWrapper: Group
    val tileFieldZoneBorder: Image
    val tileFieldBorder: Image
    val bottomSheetBg: Image
    val comboParticles: ParticleEffect

    var fingerActor: Image? = null

    private var combo = 0

    val tileFieldAreaHeight = min(Gdx.graphics.width.toFloat(), Gdx.graphics.height - Gdx.graphics.width / 1.25f - 48f * context.scale) - 32f

    private var potionPanel: AlchemyPanel? = null

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

        addActor(buttonPotions)

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
        BattleFinishedDialog(context, "Defeat", emptyList(), emptyList(), screen) {
            finishCallback(false)
        }.apply {
            x = 40f * context.scale
            y = 220f * context.scale
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
        BattleFinishedDialog(context, "Victory", expResult, rewards, screen) {
            finishCallback(true)
        }.apply {
            x = 40f * context.scale
            y = 220f * context.scale
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

    fun showFingerAnimation(dx: Int, dy: Int) {
        dismissFingerAnimation()
        fingerActor = Image(context.uiAtlas.findRegion("tutorial_swipe_finger")).apply {
            x = tileField.x + tileField.tileSize * 5 / 2f - 32f * context.scale
            y = tileField.y + tileField.tileSize * 5 / 2f - 32f * context.scale
            width = 64f * context.scale
            height = 64f * context.scale
            val distance = tileField.tileSize * 2f
            this@GameActor.addActor(this)

            this.addAction(SequenceAction(
                    MoveByAction().apply { setAmount(dx * distance, dy * distance); duration = 0.6f },
                    DelayAction(0.2f).apply { action =  MoveByAction().apply { setAmount(-dx * distance, -dy * distance); duration = 0.01f }}
            ).repeatForever())
        }
    }

    fun dismissFingerAnimation() {
        fingerActor?.let {
            it.remove()
        }
        fingerActor = null
    }

    fun hideAlchemy() {
        potionPanel?.animateHideToBottom(context, AlchemyPanel.h)
        potionPanel = null
    }

    private fun switchPotions() {
        if (potionPanel == null) {
            potionPanel = AlchemyPanel(context, gearService, AlchemyPanelMode.DrinkMode(usePotionCallback)).apply {
                y = 48f * context.scale
            }
            addActor(potionPanel)
            potionPanel?.animateShowFromBottom(context, AlchemyPanel.h)
        } else {
            hideAlchemy()
        }
    }

    fun refreshAlchemy() {
        potionPanel?.reloadData()
    }
}