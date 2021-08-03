package com.game7th.swipe.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.gdx.utils.viewport.Viewport
import com.esotericsoftware.spine.SkeletonData
import com.esotericsoftware.spine.SkeletonJson
import com.game7th.metagame.PersistentStorage
import com.game7th.metagame.campaign.ActsService
import com.game7th.metagame.inventory.GearService
import com.game7th.swipe.BaseScreen
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.SwipeGameGdx
import com.game7th.swipe.campaign.ActScreen
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

    var battleController: BattleController? = null

    lateinit var batch: SpriteBatch

    lateinit var battleContext: BattleContext

    lateinit var processor: SimpleDirectionGestureDetector

    val gdxModel = Gson().fromJson<GdxModel>(Gdx.files.internal("figures.json").readString(), GdxModel::class.java)

    var gameActor: GameActor? = null

    val figureUis = mutableMapOf<String, TextureAtlas>()
    val skeletonDatas = mutableMapOf<String, SkeletonData>()
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


        }
    }

    private fun processSwipe(dx: Int, dy: Int, prevent: Boolean) {
        KtxAsync.launch {
            if (dismissFocusOnSwipe && !prevent) {
                dismissFocusView()
            }
            if (!gameEnded && !prevent) {
                gameActor?.tileField?.finalizeActions()
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

    private fun loadResources(gdxFigure: FigureGdxModel, atlases: MutableMap<String, TextureAtlas>) {
        if (!atlases.containsKey(gdxFigure.atlas)) {
            atlases[gdxFigure.atlas] = TextureAtlas(Gdx.files.internal("textures/personages/${gdxFigure.atlas}/${gdxFigure.name}.atlas"))
            gdxFigure.dependencies?.let { dependencies ->
                dependencies.forEach { dependencyFigure ->
                    gdxModel.figures.firstOrNull { it.name == dependencyFigure }?.let { loadResources(it, atlases) }
                }
            }
            gdxFigure.attacks.forEach {
                it.sound?.let { sounds[it] = Gdx.audio.newSound(Gdx.files.internal("sounds/${it}.ogg")) }
                it.effect?.sound?.let { sounds[it] = Gdx.audio.newSound(Gdx.files.internal("sounds/${it}.ogg")) }
                it.effect?.atlas?.let { gdxModel.figure(it) }?.let { loadResources(it, atlases) }
            }
            gdxFigure.poses?.forEach {
                it.sound?.let { sounds[it] = Gdx.audio.newSound(Gdx.files.internal("sounds/${it}.ogg")) }
            }
            Gdx.files.internal("textures/personages/${gdxFigure.name}/ui.atlas").let { handle ->
                if (handle.exists()) {
                    val uiAtlas = TextureAtlas(handle)
                    gdxFigure.tiles?.forEach { figureUis[it] = uiAtlas }
                    figureUis[gdxFigure.name] = uiAtlas
                }
            }
            gdxFigure.tiles?.forEach {
                val spineAtlas = TextureAtlas(Gdx.files.internal("textures/tiles/$it/tile.atlas"))
                val json = SkeletonJson(spineAtlas)
                val jsonData = json.readSkeletonData(Gdx.files.internal("textures/tiles/$it/spine.json"))
                skeletonDatas[it] = jsonData
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
            val actConfig = actService.getActConfig(actId)
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
                runBlocking(KtxAsync.coroutineContext) {
                    gameActor?.processAction(it)
                }
                battleController?.processEvent(it)

                when (it) {
                    is BattleEvent.VictoryEvent -> onGameEnded(true)
                    is BattleEvent.DefeatEvent -> onGameEnded(false)
                    is BattleEvent.FlaskConsumedEvent -> {
                        gearService.reloadData()
                        gameActor?.refreshAlchemy()
                    }
                    is BattleEvent.BattleReadyEvent -> {
                        KtxAsync.launch {
                            val atlases = mutableMapOf<String, TextureAtlas>()
                            atlases["ailments"] = TextureAtlas(Gdx.files.internal("textures/ailments.atlas"))
                            listOf("wind").forEach {
                                sounds[it] = Gdx.audio.newSound(Gdx.files.internal("sounds/$it.ogg"))
                            }
                            it.battleInfo.figures.forEach { figureName ->
                                gdxModel.figures.firstOrNull { it.name == figureName }?.let {
                                    loadResources(it, atlases)
                                }
                            }

                            val locationConfig = actConfig.findNode(locationId)!!
                            val battleAtlas = TextureAtlas(Gdx.files.internal("textures/battle.atlas"))
                            val locationAtlas = TextureAtlas(Gdx.files.internal("textures/locations/${locationConfig.scene}.atlas"))
                            val scale = game.context.scale
                            println("atlases: ${atlases.keys}")
                            battleContext = BattleContext(
                                gameContext = game.context,
                                battleAtlas = battleAtlas,
                                locationAtlas = locationAtlas,
                                scale = scale,
                                gdxModel = gdxModel,
                                width = Gdx.graphics.width.toFloat(),
                                height = Gdx.graphics.height.toFloat(),
                                atlases = atlases,
                                skeletons = skeletonDatas,
                                figuresUi = figureUis
                            )

                            gameActor = GameActor(
                                battleContext, game.gearService, locationConfig, this@GameScreen, this@GameScreen::usePotion, this@GameScreen::claimRewards) { _ ->
                                KtxAsync.launch {
                                    game.gearService.reloadData()
                                }
                                game.switchScreen(ActScreen(game, game.actService, actId, game.context, game.storage))
                            }

                            stage.addActor(gameActor)

                            game.multiplexer.addProcessor(stage)

                            battleController = BattleController(battleContext, this@GameScreen, 480f * context.scale, sounds) {
                                if (it is BattleEvent.VictoryEvent) {
                                    gameActor?.showVictory()
                                    backgroundMusic.pause()
                                } else {
                                    gameActor?.showDefeat()
                                    backgroundMusic.pause()
                                }
                            }

                            backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("sb_chase.ogg")).apply {
                                volume = 0.5f
                                isLooping = true
                                play()
                            }

                            ready = true
                            isResoucesReady = true
                            swipeFlow.emit(InputBattleEvent.PlayerReadyEvent(accountId))
                        }
                    }
                    is BattleEvent.NewWaveEvent -> {
                    }
                    is BattleEvent.HeartbeatResponse -> {
                        gameActor?.showPing(pingTimeMark.elapsedNow().inMilliseconds.toLong())
                    }
                }
            }
        }
    }

    private fun onGameEnded(victory: Boolean) {
        gameEnded = true
    }

    override fun render(delta: Float) {
        if (ready) {
            batch.begin()
            battleController?.act(batch, delta)
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
        battleContext.atlases.forEach { (_, atlas) -> atlas.dispose() }
    }
}