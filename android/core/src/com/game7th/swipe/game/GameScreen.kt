package com.game7th.swipe.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.gdx.utils.viewport.Viewport
import com.game7th.metagame.PersistentStorage
import com.game7th.metagame.campaign.ActsService
import com.game7th.metagame.dto.UnitType
import com.game7th.metagame.inventory.GearService
import com.game7th.swipe.BaseScreen
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.SwipeGameGdx
import com.game7th.swipe.TutorialKeys
import com.game7th.swipe.campaign.ActScreen
import com.game7th.swipe.dialog.DismissStrategy
import com.game7th.swipe.game.actors.GameActor
import com.game7th.swipe.game.battle.BattleController
import com.game7th.swipe.game.battle.model.FigureGdxModel
import com.game7th.swipe.game.battle.model.GdxModel
import com.game7th.swipe.gestures.SimpleDirectionGestureDetector
import com.game7th.swiped.api.FlaskItemFullInfoDto
import com.game7th.swiped.api.LocationCompleteResponseDto
import com.game7th.swiped.api.PersonageDto
import com.game7th.swiped.api.battle.BattleEvent
import com.game7th.swiped.api.battle.InputBattleEvent
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import ktx.async.KtxAsync
import kotlin.time.ExperimentalTime
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class GameScreen(game: SwipeGameGdx,
                 private val battleId: String,
                 private val accountId: String,
                 private val actId: String,
                 private val locationId: Int,
                 private val difficulty: Int,
                 private val personage: PersonageDto,
                 private val actService: ActsService,
                 private val gearService: GearService,
                 private val storage: PersistentStorage,
                 gdxGameContext: GdxGameContext
) : BaseScreen(gdxGameContext, game) {

    lateinit var viewport: Viewport

    lateinit var battleController: BattleController

    lateinit var batch: SpriteBatch

    lateinit var processor: SimpleDirectionGestureDetector

    val gdxModel = Gson().fromJson<GdxModel>(Gdx.files.internal("figures.json").readString(), GdxModel::class.java)

    lateinit var gameActor: GameActor
    val atlases = mutableMapOf<String, TextureAtlas>()
    val sounds = mutableMapOf<String, Sound>()

    lateinit var backgroundMusic: Music

    val swipeFlow = MutableSharedFlow<InputBattleEvent>()

    var gameEnded = false
    var preventLeftSwipe = false
    var preventRightSwipe = false
    var preventBottomSwipe = false
    var preventTopSwipe = false
    var dismissFocusOnSwipe = false

    @Volatile
    var ready = false

    @ExperimentalTime
    lateinit var pingTimeMark: TimeMark
    var isResoucesReady = false

    @ExperimentalTime
    override fun show() {
        super.show()
        viewport = ScreenViewport()

        batch = SpriteBatch()

        processor = createSwipeDetector()
        game.multiplexer.addProcessor(0, processor)

        KtxAsync.launch {

            listenEvents()

            gameActor = GameActor(
                    game.context, game.gearService, this@GameScreen, this@GameScreen::usePotion, this@GameScreen::claimRewards) { _ ->
                KtxAsync.launch {
                    game.gearService.reloadData()
                }
                game.switchScreen(ActScreen(game, game.actService, actId, game.context, game.storage))
            }

            stage.addActor(gameActor)

            game.multiplexer.addProcessor(stage)

            val scale = game.context.scale
            battleController = BattleController(GameContextWrapper(
                    gameContext = game.context,
                    scale = scale,
                    gdxModel = gdxModel,
                    width = Gdx.graphics.width.toFloat(),
                    height = Gdx.graphics.height.toFloat(),
                    atlases = atlases
            ), this@GameScreen, 480f * context.scale, sounds) {
                if (it is BattleEvent.VictoryEvent) {
                    gameActor.showVictory()
                    backgroundMusic.pause()
                } else {
                    gameActor.showDefeat()
                    backgroundMusic.pause()
                }
            }

            backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("sb_chase.ogg")).apply {
                volume = 0.5f
                isLooping = true
                play()
            }

            ready = true
        }
    }

    private fun processSwipe(dx: Int, dy: Int, prevent: Boolean) {
        KtxAsync.launch {
            if (dismissFocusOnSwipe && !prevent) {
                dismissFocusView()
            }
            if (!gameEnded && !prevent) {
                gameActor.tileField.finalizeActions()
                swipeFlow.emit(InputBattleEvent.SwipeBattleEvent(dx, dy))
            }
        }
    }

    private fun createSwipeDetector() =
            SimpleDirectionGestureDetector(object : SimpleDirectionGestureDetector.DirectionListener {
                override fun onLeft() { processSwipe(-1, 0, preventLeftSwipe) }
                override fun onRight() { processSwipe(1, 0, preventRightSwipe) }
                override fun onUp() { processSwipe(0, -1, preventTopSwipe) }
                override fun onDown() { processSwipe(0, 1, preventBottomSwipe) }
            })

    private fun loadResources(gdxFigure: FigureGdxModel) {
        if (!atlases.containsKey(gdxFigure.atlas)) {
            atlases[gdxFigure.atlas] = TextureAtlas(Gdx.files.internal("${gdxFigure.name}.atlas"))
            gdxFigure.dependencies?.let { dependencies ->
                dependencies.forEach { dependencyFigure ->
                    gdxModel.figures.firstOrNull { it.name == dependencyFigure }?.let { loadResources(it) }
                }
            }
            gdxFigure.attacks.forEach {
                it.sound?.let { sounds[it] = Gdx.audio.newSound(Gdx.files.internal("sounds/${it}.ogg")) }
                it.effect?.sound?.let { sounds[it] = Gdx.audio.newSound(Gdx.files.internal("sounds/${it}.ogg")) }
                it.effect?.atlas?.let { gdxModel.figure(it) }?.let { loadResources(it) }
            }
            gdxFigure.poses.forEach {
                it.sound?.let { sounds[it] = Gdx.audio.newSound(Gdx.files.internal("sounds/${it}.ogg")) }
            }
        }

    }

    private fun usePotion(flask: FlaskItemFullInfoDto) {
        KtxAsync.launch {
            flask.id?.let { flaskId ->
                swipeFlow.emit(InputBattleEvent.FlaskBattleEvent(flaskId))
            }
        }
    }

    private suspend fun claimRewards(): LocationCompleteResponseDto {
        game.accountService.refreshPersonages()
        return actService.markLocationComplete(actId, locationId, difficulty, personage.id)
    }

    //    private fun produceTutorials(): List<AbilityTrigger> {
//        val result = mutableListOf<AbilityTrigger>()
//        if (actId == "act_0" && locationId == 10 && TutorialKeys.tutorialsEnabled && storage.get(TutorialKeys.ACT1_L11_TALK)?.toBoolean() != true) { result.add(Act1L11Talk(this, game)) }
//        if (actId == "act_0" && locationId == 13 && TutorialKeys.tutorialsEnabled && storage.get(TutorialKeys.ACT1_L14_TALK)?.toBoolean() != true) { result.add(Act1L14Talk(this, game)) }
//        return result
//    }

    @ExperimentalTime
    private fun listenEvents() {
        KtxAsync.launch {
            launch {
                repeat(1000) { i ->
                    if (isResoucesReady && !gameEnded) {
                        pingTimeMark = TimeSource.Monotonic.markNow()
                        swipeFlow.emit(InputBattleEvent.HeartBeatEvent(accountId))
                    }
                    delay(2000L)
                }
            }
            game.api.connectBattle(accountId, battleId, swipeFlow) {
                println("S7TH GS: THREAD: ${Thread.currentThread().name}")
                gameActor.processAction(it)
                battleController.processEvent(it)

                when (it) {
                    is BattleEvent.VictoryEvent -> onGameEnded(true)
                    is BattleEvent.DefeatEvent -> onGameEnded(false)
                    is BattleEvent.FlaskConsumedEvent -> {
                        gearService.reloadData()
                        gameActor.refreshAlchemy()
                    }
                    is BattleEvent.BattleReadyEvent -> {
                        val resourceLoader = KtxAsync.async {
                            atlases["ailments"] = TextureAtlas(Gdx.files.internal("ailments.atlas"))
                            listOf("wind").forEach {
                                sounds[it] = Gdx.audio.newSound(Gdx.files.internal("sounds/$it.ogg"))
                            }
                            it.battleInfo.figures.forEach { figureName ->
                                gdxModel.figures.firstOrNull { it.name == figureName }?.let {
                                    loadResources(it)
                                }
                            }
                        }
                        resourceLoader.join()
                        isResoucesReady = true
                        swipeFlow.emit(InputBattleEvent.PlayerReadyEvent(accountId))
                    }
                    is BattleEvent.NewWaveEvent -> {
                        T_A0L1()
                        T_A0L3()
                        T_A0L4()
                        T_A0L5()
                        T_A0L7()
                        T_A0L8(it)
                        T_A0L9(it)
                        T_A0L11(it)
                        T_A0L14(it)
                    }
                    is BattleEvent.HeartbeatResponse -> {
                        gameActor.showPing(pingTimeMark.elapsedNow().inMilliseconds.toLong())
                    }
                }
            }
        }
    }

    private fun T_A0L14(it: BattleEvent.NewWaveEvent) {
        if (actId == "act_0" && locationId == 13 && TutorialKeys.tutorialsEnabled && storage.get(TutorialKeys.ACT1_L14_TALK)?.toBoolean() != true && it.wave == 0) {
            preventBottomSwipe = true
            preventTopSwipe = true
            preventLeftSwipe = true
            preventRightSwipe = true

            showDialog("vp_bhastuse_jolly", "Bhastuse", game.context.texts["ttr_a1l14_fb_1"]!!) {
                showDialog("vp_strange_figure", "Strange Figure", game.context.texts["ttr_a1l14_fb_2"]!!) {
                    showDialog("vp_bhastuse_jolly", "Bhastuse", game.context.texts["ttr_a1l14_fb_3"]!!) {
                        preventBottomSwipe = false
                        preventTopSwipe = false
                        preventLeftSwipe = false
                        preventRightSwipe = false
                        game.storage.put(TutorialKeys.ACT1_L14_TALK, true.toString())
                    }
                }
            }
        }
    }

    private fun T_A0L11(it: BattleEvent.NewWaveEvent) {
        if (actId == "act_0" && locationId == 10 && TutorialKeys.tutorialsEnabled && storage.get(TutorialKeys.ACT1_L11_TALK)?.toBoolean() != true && it.wave == 0) {
            preventBottomSwipe = true
            preventTopSwipe = true
            preventLeftSwipe = true
            preventRightSwipe = true

            showDialog("vp_personage_gladiator", "Antoxa", game.context.texts["ttr_a1l11_fb_1"]!!) {
                showDialog("vp_dryad", "Dryad", game.context.texts["ttr_a1l11_fb_2"]!!) {
                    showDialog("vp_personage_gladiator", "Antoxa", game.context.texts["ttr_a1l11_fb_3"]!!) {
                        showDialog("vp_strange_figure", "Strange Figure", game.context.texts["ttr_a1l11_fb_4"]!!) {
                            preventBottomSwipe = false
                            preventTopSwipe = false
                            preventLeftSwipe = false
                            preventRightSwipe = false
                            game.storage.put(TutorialKeys.ACT1_L11_TALK, true.toString())
                        }
                    }
                }
            }
        }
    }

    private fun T_A0L9(it: BattleEvent.NewWaveEvent) {
        if (actId == "act_0" && locationId == 8 && TutorialKeys.tutorialsEnabled && storage.get(TutorialKeys.ACT1_L9_TALK)?.toBoolean() != true) {
            if (it.wave == 0) {
                preventBottomSwipe = true
                preventTopSwipe = true
                preventLeftSwipe = true
                preventRightSwipe = true

                showDialog("vp_bhastuse_jolly", "Bhastuse", game.context.texts["ttr_a1l9_fb_1"]!!) {
                    showDialog("vp_personage_gladiator", "Antoxa", game.context.texts["ttr_a1l9_fb_2"]!!) {
                        showDialog("vp_bhastuse_jolly", "Bhastuse", game.context.texts["ttr_a1l9_fb_3"]!!) {
                            preventBottomSwipe = false
                            preventTopSwipe = false
                            preventLeftSwipe = false
                            preventRightSwipe = false
                        }
                    }
                }
            } else if (it.wave == 1) {
                preventBottomSwipe = true
                preventTopSwipe = true
                preventLeftSwipe = true
                preventRightSwipe = true
                showDialog("vp_bhastuse_jolly", "Bhastuse", game.context.texts["ttr_a1l9_fb_4"]!!) {
                    preventBottomSwipe = false
                    preventTopSwipe = false
                    preventLeftSwipe = false
                    preventRightSwipe = false
                    game.storage.put(TutorialKeys.ACT1_L9_TALK, true.toString())
                }
            }
        }
    }

    private fun T_A0L8(event: BattleEvent.NewWaveEvent) {
        if (actId == "act_0" && locationId == 7 && TutorialKeys.tutorialsEnabled && storage.get(TutorialKeys.ACT1_L8_TALK)?.toBoolean() != true) {
            if (event.wave == 0) {
                preventBottomSwipe = true
                preventTopSwipe = true
                preventLeftSwipe = true
                preventRightSwipe = true

                showDialog("vp_personage_gladiator", "Antoxa", game.context.texts["ttr_a1l8_fb_1"]!!) {
                    showDialog("vp_bhastuse_jolly", "Bhastuse", game.context.texts["ttr_a1l8_fb_2"]!!) {
                        showDialog("vp_strange_figure", "Strange Figure", game.context.texts["ttr_a1l8_fb_3"]!!) {
                            preventBottomSwipe = false
                            preventTopSwipe = false
                            preventLeftSwipe = false
                            preventRightSwipe = false
                        }
                    }
                }
            } else if (event.wave == 1) {
                preventBottomSwipe = true
                preventTopSwipe = true
                preventLeftSwipe = true
                preventRightSwipe = true
                showDialog("vp_strange_figure", "Strange Figure", game.context.texts["ttr_a1l8_fb_4"]!!) {
                    showDialog("vp_personage_gladiator", "Strange Figure", game.context.texts["ttr_a1l8_fb_5"]!!) {
                        preventBottomSwipe = false
                        preventTopSwipe = false
                        preventLeftSwipe = false
                        preventRightSwipe = false
                        game.storage.put(TutorialKeys.ACT1_L8_TALK, true.toString())
                    }
                }
            }
        }
    }

    private fun T_A0L7() {
        if (actId == "act_0" && locationId == 6 && TutorialKeys.tutorialsEnabled && storage.get(TutorialKeys.ACT1_L7_TALK)?.toBoolean() != true) {
            preventBottomSwipe = true
            preventTopSwipe = true
            preventLeftSwipe = true
            preventRightSwipe = true

            showDialog("vp_personage_gladiator", "Strange Figure", game.context.texts["ttr_a1l7_fb_1"]!!) {
                showDialog("vp_bhastuse_jolly", "Bhastuse", game.context.texts["ttr_a1l7_fb_2"]!!) {
                    showDialog("vp_strange_figure", "Strange Figure", game.context.texts["ttr_a1l7_fb_3"]!!) {
                        preventBottomSwipe = false
                        preventTopSwipe = false
                        preventLeftSwipe = false
                        preventRightSwipe = false
                        game.storage.put(TutorialKeys.ACT1_L7_TALK, true.toString())
                    }
                }
            }
        }
    }

    private fun T_A0L5() {
        if (actId == "act_0" && locationId == 4 && TutorialKeys.tutorialsEnabled && storage.get(TutorialKeys.ACT1_L5_TALK)?.toBoolean() != true) {
            preventBottomSwipe = true
            preventTopSwipe = true
            preventLeftSwipe = true
            preventRightSwipe = true

            showDialog("vp_strange_figure", "Strange Figure", game.context.texts["ttr_a1l5_fb_1"]!!) {
                showDialog("vp_personage_gladiator", "Antoxa", game.context.texts["ttr_a1l5_fb_2"]!!) {
                    showDialog("vp_strange_figure", "Strange Figure", game.context.texts["ttr_a1l5_fb_3"]!!) {
                        showDialog("vp_personage_gladiator", "Antoxa", game.context.texts["ttr_a1l5_fb_4"]!!) {
                            preventBottomSwipe = false
                            preventTopSwipe = false
                            preventLeftSwipe = false
                            preventRightSwipe = false
                            game.storage.put(TutorialKeys.ACT1_L5_TALK, true.toString())
                        }
                    }
                }
            }
        }
    }

    private fun T_A0L4() {
        if (actId == "act_0" && locationId == 3 && TutorialKeys.tutorialsEnabled && storage.get(TutorialKeys.ACT1_L4_TALK)?.toBoolean() != true) {
            preventBottomSwipe = true
            preventTopSwipe = true
            preventLeftSwipe = true
            preventRightSwipe = true

            showDialog("vp_personage_gladiator", "Antoxa", game.context.texts["ttr_a1l4_fb_1"]!!) {
                showDialog("vp_strange_figure", "Strange figure", game.context.texts["ttr_a1l4_fb_2"]!!) {
                    showDialog("vp_personage_gladiator", "Antoxa", game.context.texts["ttr_a1l4_fb_3"]!!) {
                        showDialog("vp_strange_figure", "Strange figure", game.context.texts["ttr_a1l4_fb_4"]!!) {
                            showDialog("vp_bhastuse_jolly", "Bhastuse", game.context.texts["ttr_a1l4_fb_5"]!!) {
                                showDialog("vp_personage_gladiator", "Antoxa", game.context.texts["ttr_a1l4_fb_6"]!!) {
                                    showDialog("vp_strange_figure", "Strange figure", game.context.texts["ttr_a1l4_fb_7"]!!) {
                                        preventBottomSwipe = false
                                        preventTopSwipe = false
                                        preventLeftSwipe = false
                                        preventRightSwipe = false
                                        game.storage.put(TutorialKeys.ACT1_L4_TALK, true.toString())
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun T_A0L3() {
        if (actId == "act_0" && locationId == 2 && TutorialKeys.tutorialsEnabled && storage.get(TutorialKeys.ACT1_L3_TALK)?.toBoolean() != true) {
            preventBottomSwipe = true
            preventTopSwipe = true
            preventLeftSwipe = true
            preventRightSwipe = true

            showDialog("vp_strange_figure", "Strange Figure", game.context.texts["ttr_a1l3_fb_1"]!!) {
                showDialog("vp_personage_gladiator", "Antoxa", game.context.texts["ttr_a1l3_fb_2"]!!) {
                    showDialog("vp_strange_figure", "Strange Figure", game.context.texts["ttr_a1l3_fb_3"]!!) {
                        preventBottomSwipe = false
                        preventTopSwipe = false
                        preventLeftSwipe = false
                        preventRightSwipe = false
                        game.storage.put(TutorialKeys.ACT1_L3_TALK, true.toString())
                    }
                }
            }
        }
    }

    private fun T_A0L1() {
        if (actId == "act_0" &&
                locationId == 0 &&
                TutorialKeys.tutorialsEnabled &&
                storage.get(TutorialKeys.ACT1_FIRST_BATTLE_INTRO_SHOWN)?.toBoolean() != true &&
                personage.level == 1 &&
                personage.unit == UnitType.GLADIATOR.toString()) {
            preventBottomSwipe = true
            preventTopSwipe = true
            preventLeftSwipe = true
            preventRightSwipe = true

            showDialog("vp_personage_gladiator", "Antoxa", game.context.texts["ttr_a1l1_fb_1"]!!) {
                showDialog("vp_strange_figure", "Strange Figure", game.context.texts["ttr_a1l1_fb_2"]!!) {
                    showDialog("vp_personage_gladiator", "Antoxa", game.context.texts["ttr_a1l1_fb_3"]!!) {
                        showDialog("vp_strange_figure", "Strange Figure", game.context.texts["ttr_a1l1_fb_4"]!!) {
                            showFocusView("ttr_a1l1_fb_5", calcLeftPersonageRect(), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                showFocusView("ttr_a1l1_fb_6", calcRightPersonageRect(), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                    showFocusView("ttr_a1l1_fb_7", calcLeftPersonageHpBarRect(), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                        showFocusView("ttr_a1l1_fb_8", calcRightPersonageHpBarRect(), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                            showFocusView("ttr_a1l1_fb_9", calcTileFieldRect(), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                                showFocusView("ttr_a1l1_fb_10", calcTileRect(7), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                                    showFocusView("ttr_a1l1_fb_11", calcTileRect(9), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                                        showFocusView("ttr_a1l1_fb_12", calcTileRect(17), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                                            preventLeftSwipe = false
                                                            dismissFocusOnSwipe = true
                                                            showFingerAnimation(-1, 0)
                                                            showFocusView("ttr_a1l1_fb_13", calcTileFieldRect(), DismissStrategy.DISMISS_FORCED) {
                                                                dismissFingerAnimation()
                                                                preventLeftSwipe = true
                                                                dismissFocusOnSwipe = false
                                                                showFocusView("ttr_a1l1_fb_14", calcTileRect(5), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                                                    showFocusView("ttr_a1l1_fb_15", calcComboRect(), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                                                        showFocusView("ttr_a1l1_fb_16", calcTileRect(13), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                                                            showFocusView("ttr_a1l1_fb_17", calcRightPersonageSkillRect(), DismissStrategy.DISMISS_ON_OUTSIDE)
                                                                            preventBottomSwipe = false
                                                                            dismissFocusOnSwipe = true
                                                                            showFingerAnimation(0, -1)
                                                                            showFocusView("ttr_a1l1_fb_18", calcTileFieldRect(), DismissStrategy.DISMISS_FORCED) {
                                                                                dismissFingerAnimation()
                                                                                dismissFocusOnSwipe = false
                                                                                preventBottomSwipe = true
                                                                                showDialog("vp_strange_figure", "Strange Figure", game.context.texts["ttr_a1l1_fb_19"]!!) {
                                                                                    showDialog("vp_personage_gladiator", "Antoxa", game.context.texts["ttr_a1l1_fb_20"]!!) {
                                                                                        showDialog("vp_strange_figure", "Strange Figure", game.context.texts["ttr_a1l1_fb_21"]!!) {
                                                                                            preventBottomSwipe = false
                                                                                            preventTopSwipe = false
                                                                                            preventLeftSwipe = false
                                                                                            preventRightSwipe = false
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun onGameEnded(victory: Boolean) {
        gameEnded = true
        if (actId == "act_0" && locationId == 0) {
            storage.put(TutorialKeys.ACT1_FIRST_BATTLE_INTRO_SHOWN, true.toString())
        }
    }

    override fun render(delta: Float) {
        if (ready) {
            batch.begin()
            battleController.act(batch, delta)
            batch.end()
        }

        super.render(delta)
    }

    override fun resize(width: Int, height: Int) {
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun hide() {
        stage.dispose()
        sounds.forEach { it.value.dispose() }
        backgroundMusic.stop()
        backgroundMusic.dispose()
    }

    override fun dispose() {
        game.multiplexer.removeProcessor(processor)
        atlases.forEach { (_, atlas) ->
            atlas.dispose()
        }
    }



    fun calcLeftPersonageRect() = battleController.calcLeftPersonageRect()
    fun calcRightPersonageRect() = battleController.calcRightPersonageRect()
    fun calcLeftPersonageHpBarRect() = battleController.calcLeftPersonageHpBarRect()
    fun calcRightPersonageHpBarRect() = battleController.calcRightPersonageHpBarRect()
    fun calcTileFieldRect() = gameActor.tileField.localToStageCoordinates(Vector2()).let {
        Rectangle(it.x - 5f, it.y - 5f, gameActor.tileField.tileSize * 5 + 10f, gameActor.tileField.tileSize * 5 + 10f)
    }

    fun calcTileRect(p: Int) = gameActor.tileField.localToStageCoordinates(Vector2()).let {
        Rectangle(it.x + gameActor.tileField.calcX(p) - 5f, it.y + gameActor.tileField.calcY(p) - 5f, gameActor.tileField.tileSize + 10f, gameActor.tileField.tileSize + 10f)
    }

    fun calcComboRect(): Rectangle = gameActor.labelCombo.localToStageCoordinates(Vector2()).let {
        Rectangle(it.x - 30f, it.y - 30f, gameActor.labelCombo.width + 30f, gameActor.labelCombo.height + 30f)
    }

    fun calcRightPersonageSkillRect() = battleController.calcRightPersonageSkillRect()
    fun showFingerAnimation(dx: Int, dy: Int) = gameActor.showFingerAnimation(dx, dy)
    fun dismissFingerAnimation() = gameActor.dismissFingerAnimation()
}