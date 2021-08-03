package com.game7th.swipe.game.actors

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.*
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.game7th.metagame.campaign.dto.LocationConfig
import com.game7th.metagame.inventory.GearService
import com.game7th.swipe.game.BattleContext
import com.game7th.swipe.game.GameScreen
import com.game7th.swipe.game.actors.ui.BattleFinishedDialog
import com.game7th.swiped.api.FlaskItemFullInfoDto
import com.game7th.swiped.api.LocationCompleteResponseDto
import com.game7th.swiped.api.RewardListDto
import com.game7th.swiped.api.battle.BattleEvent
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ktx.actors.onClick
import ktx.actors.repeatForever
import ktx.async.KtxAsync

class GameActor(
        private val context: BattleContext,
        private val gearService: GearService,
        private val locationConfig: LocationConfig,
        private val screen: GameScreen,
        private val usePotionCallback: (FlaskItemFullInfoDto) -> Unit,
        private val rewardCallback: suspend () -> LocationCompleteResponseDto,
        private val finishCallback: (Boolean) -> Unit
) : Group() {

    lateinit var atlas: TextureAtlas
    val tileField: TileFieldView
    val flaskPanel = Group()

    val scaleIconCombo = Image(context.battleAtlas.findRegion("icon_mind")).apply {
        x = 0f
        y = 60f * context.scale
        width = 70f * context.scale
        height = 88f * context.scale
    }
    val scaleIconUltimate = Image(context.battleAtlas.findRegion("icon_spirit")).apply {
        x = 413f * context.scale
        y = 58f * context.scale
        width = 67f * context.scale
        height = 82f * context.scale
    }
    val scaleCombo = ScaleActor(context, "scale_wisdom", 10).apply {
        x = scaleIconCombo.x + 27f * context.scale
        y = scaleIconCombo.y + 68f * context.scale
    }
    val scaleUltimate = ScaleActor(context, "scale_combo", 20).apply {
        x = scaleIconUltimate.x + 26f * context.scale
        y = scaleIconUltimate.y + 70f * context.scale
    }
    val comboCaption = Label(context.gameContext.texts["ui_combo"], Label.LabelStyle(context.gameContext.captionFont, Color.YELLOW)).apply {
        x = scaleIconCombo.x
        y = scaleIconCombo.y + 9f * context.scale
        height = 12f * context.scale
        setFontScale(12f * context.scale / 36f)
        width = scaleIconCombo.width
        setAlignment(Align.center)
        isVisible = false
    }
    val comboValue = Label("X1", Label.LabelStyle(context.gameContext.captionFont, Color.YELLOW)).apply {
        x = scaleIconCombo.x
        y = scaleIconCombo.y - 11f * context.scale
        height = 20f * context.scale
        setFontScale(20f * context.scale / 36f)
        width = scaleIconCombo.width
        setAlignment(Align.center)
        isVisible = false
    }
    val uiGroup = Group()
    val buttonSettings = Button(Button.ButtonStyle(
            TextureRegionDrawable(context.battleAtlas.findRegion("btn_settings")),
            TextureRegionDrawable(context.battleAtlas.findRegion("btn_settings_pressed")),
            null)).apply {
                x = Gdx.graphics.width - 87f * context.scale
                y = Gdx.graphics.height - 87f * context.scale
                width = 60f * context.scale
                height = 60f * context.scale
                onClick { showDefeat() }
    }
    val progressIndicator = ProgressIndicatorActor(
            context,
            ProgressConfiguration(0, locationConfig.waves.map { WaveInfo(WaveType.REGULAR, false) } + WaveInfo(WaveType.REWARD, false))
        ).apply {
        y = Gdx.graphics.height - 60f * context.scale
        x = (Gdx.graphics.width - ProgressIndicatorActor.WIDTH * context.scale) / 2f
    }

    var fingerActor: Image? = null

    private var combo = 0

    val tileFieldAreaHeight = 348f * context.scale

    val pingLabel = Label("DEBUG", Label.LabelStyle(context.gameContext.regularFont, Color.BLUE)).apply {
        x = 20f * context.scale
        y = Gdx.graphics.height - context.scale * 16f
        setFontScale(context.scale / 2f)
        width = context.scale * 100f
        height = context.scale * 16f
        touchable = Touchable.disabled
    }

    init {
        val bottomPanelHei = 480f * context.scale / 9.66f

        tileField = TileFieldView(context, tileFieldAreaHeight, tileFieldAreaHeight).apply {
            x = (Gdx.graphics.width - tileFieldAreaHeight) / 2f
            y = 84f * context.scale
        }

        flaskPanel.apply {
            x = 68f * context.scale
            y = 10f * context.scale
        }
        (0..4).forEach { index ->
            val nodeView = BoosterNodeView(context, 60f * context.scale, usePotionCallback)
            nodeView.x = index * 70f * context.scale
            flaskPanel.addActor(nodeView)
        }

        addActor(tileField)
        addActor(flaskPanel)
        refreshAlchemy()
        addActor(scaleCombo)
        addActor(scaleUltimate)
        addActor(scaleIconCombo)
        addActor(scaleIconUltimate)
        addActor(comboCaption)
        addActor(comboValue)
        addActor(buttonSettings)
        addActor(progressIndicator)

        addActor(uiGroup)
        addActor(pingLabel)
    }

    internal fun showDefeat() {
        tileField.touchable = Touchable.disabled

        BattleFinishedDialog(context.gameContext, "Defeat", LocationCompleteResponseDto(RewardListDto(emptyList(), emptyList()), emptyList()), screen) {
            finishCallback(false)
        }.apply {
            x = 40f * context.scale
            y = 220f * context.scale
            this@GameActor.addActor(this)
        }
    }

    fun showVictory() {
        progressIndicator.updateCurrentLevel(progressIndicator.configuration.waves.size - 1)
//        Gdx.audio.newMusic(Gdx.files.internal("sounds/victory.ogg")).let { it.play() }
        KtxAsync.launch {
            val rewards = rewardCallback()
            BattleFinishedDialog(context.gameContext, "Victory", rewards, screen) {
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
                scaleCombo.applyProgress(event.combo * 10)
                comboCaption.isVisible = event.combo > 0
                comboValue.isVisible = event.combo > 0
                comboValue.setText("X${event.combo}")
            }
            is BattleEvent.WisdomUpdateEvent ->  {
//                scaleWisdom.applyProgress(event.wisdomProgress)
            }
            is BattleEvent.NewWaveEvent -> {
                val waveText = Label("WAVE ${event.wave+1}", Label.LabelStyle(context.gameContext.captionFont, Color.YELLOW)).apply {
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
                progressIndicator.updateCurrentLevel(event.wave)
            }
        }
    }

    fun showFingerAnimation(dx: Int, dy: Int) {
        dismissFingerAnimation()
        fingerActor = Image(context.gameContext.commonAtlas.findRegion("tutorial_swipe_finger")).apply {
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