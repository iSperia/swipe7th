package com.game7th.swipe.game.actors

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.ParticleEffect
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.*
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.game7th.metagame.inventory.GearService
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.game.GameScreen
import com.game7th.swipe.game.actors.ui.BattleFinishedDialog
import com.game7th.swiped.api.FlaskItemFullInfoDto
import com.game7th.swiped.api.LocationCompleteResponseDto
import com.game7th.swiped.api.RewardListDto
import com.game7th.swiped.api.battle.BattleEvent
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ktx.actors.repeatForever
import ktx.async.KtxAsync
import kotlin.math.min

class GameActor(
        private val context: GdxGameContext,
        private val gearService: GearService,
        private val screen: GameScreen,
        private val usePotionCallback: (FlaskItemFullInfoDto) -> Unit,
        private val rewardCallback: suspend () -> LocationCompleteResponseDto,
        private val finishCallback: (Boolean) -> Unit
) : Group() {

    lateinit var atlas: TextureAtlas
    val tileField: TileFieldView
    val flaskPanel = Group()
    val tileFieldBackground = Image(context.battleAtlas.findRegion("tilefieldbg"))
    val tileFieldForeground = Image(context.battleAtlas.findRegion("tilefield_border"))

    private val buttonConcede = BigButtonGroup(context, 80f * context.scale, 90f * context.scale, "btn_se_bg", "btn_se_bg_pressed", "btn_se_fg", "pic_concede", "pic_concede_pressed", Align.right) {
        showDefeat()
    }.apply {
        x = context.scale * (480f - 80f)
    }
    private val buttonSettings = BigButtonGroup(context, 80f * context.scale, 90f * context.scale, "btn_sw_bg", "btn_sw_bg_pressed", "btn_sw_fg", "pic_settings", "pic_settings_pressed", Align.left) {

    }

    val bottomPanel = Image(context.battleAtlas.findRegion("flask_pane_bg"))
    val labelCombo: Label
    val labelComboWrapper: Group
    val comboParticles: ParticleEffect
    val uiGroup = Group()

    var fingerActor: Image? = null

    private var combo = 0

    val tileFieldAreaHeight = 400f * context.scale

    val pingLabel = Label("DEBUG", Label.LabelStyle(context.font, Color.BLUE)).apply {
        x = 20f * context.scale
        y = Gdx.graphics.height - context.scale * 16f
        setFontScale(context.scale / 2f)
        width = context.scale * 100f
        height = context.scale * 16f
        touchable = Touchable.disabled
    }

    init {
        val bottomPanelHei = 480f * context.scale / 9.66f
        bottomPanel.apply {
            width = context.scale * 480f
            height = bottomPanelHei
        }
        tileFieldForeground.apply {
            y = bottomPanelHei - 24f * context.scale
            width = context.scale * 480f
            height = context.scale * 480f / 1.05f
        }

        tileField = TileFieldView(context, tileFieldAreaHeight, tileFieldAreaHeight).apply {
            x = (Gdx.graphics.width - tileFieldAreaHeight) / 2f
            y = tileFieldForeground.y + 30f * context.scale
        }
        tileFieldBackground.apply {
            x = tileField.x - 20f * context.scale
            y = tileField.y - 20f * context.scale
            width = tileFieldAreaHeight + 40f * context.scale
            height = tileFieldAreaHeight + 40f * context.scale
        }

        flaskPanel.apply {
            x = 90f * context.scale
            y = -5f * context.scale
        }
        (0..4).forEach { index ->
            val nodeView = BoosterNodeView(context, 60f * context.scale, usePotionCallback)
            nodeView.x = index * 60f * context.scale
            flaskPanel.addActor(nodeView)
        }

        addActor(tileFieldBackground)
        addActor(tileField)
        addActor(tileFieldForeground)
        addActor(bottomPanel)
        addActor(flaskPanel)
        addActor(buttonConcede)
        addActor(buttonSettings)
        refreshAlchemy()

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

        addActor(uiGroup)
        addActor(pingLabel)

        comboParticles = ParticleEffect()
        comboParticles.load(Gdx.files.internal("particles_0"), context.battleAtlas)
        comboParticles.setPosition(labelComboWrapper.x, labelComboWrapper.y + labelCombo.y - 10f * context.scale)
        comboParticles.scaleEffect(3f)
    }

    internal fun showDefeat() {
        tileField.touchable = Touchable.disabled

//        Gdx.audio.newMusic(Gdx.files.internal("sounds/defeat.ogg")).let { it.play() }
        BattleFinishedDialog(context, "Defeat", LocationCompleteResponseDto(RewardListDto(emptyList(), emptyList()), emptyList()), screen) {
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

    fun showVictory() {
//        Gdx.audio.newMusic(Gdx.files.internal("sounds/victory.ogg")).let { it.play() }
        KtxAsync.launch {
            val rewards = rewardCallback()
            BattleFinishedDialog(context, "Victory", rewards, screen) {
                finishCallback(true)
            }.apply {
                x = 40f * context.scale
                y = 220f * context.scale
                this@GameActor.addActor(this)
            }
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
            is BattleEvent.NewWaveEvent -> {
                val waveText = Label("WAVE ${event.wave+1}", Label.LabelStyle(context.font2, Color.YELLOW)).apply {
                    y = Gdx.graphics.height / 2f - 50f * context.scale
                    x = 0f
                    width = 480f * context.scale
                    height = 100f * context.scale
                    setAlignment(Align.center)
                    setFontScale(100f * context.scale / 36f)
                }
                uiGroup.addActor(waveText)
                waveText.addAction(SequenceAction(
                        ParallelAction(
                                MoveByAction().apply { setAmount(0f, 100f * context.scale);duration=2f },
                                AlphaAction().apply { alpha = 0f;duration=2f }
                        ),
                        RunnableAction().apply { setRunnable {
                            waveText.remove()
                        } }
                ))
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

    fun refreshAlchemy() {
        KtxAsync.launch {
            gearService.listFlasks().take(5).forEachIndexed { index, flask ->
                (flaskPanel.getChild(index) as BoosterNodeView).applyFlask(flask)
            }
        }
    }

    suspend fun showPing(ping: Long) {
        withContext(KtxAsync.coroutineContext) {
            pingLabel.setText("$ping ms")
        }
    }
}