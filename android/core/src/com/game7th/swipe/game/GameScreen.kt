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
import com.game7th.metagame.inventory.GearService
import com.game7th.swipe.BaseScreen
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.SwipeGameGdx
import com.game7th.swipe.TutorialKeys
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
import com.game7th.swiped.api.battle.BattleResultDto
import com.game7th.swiped.api.battle.InputBattleEvent
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import ktx.async.KtxAsync

class GameScreen(game: SwipeGameGdx,
                 private val battleResources: BattleResultDto,
                 private val battleId: String,
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

    var ready = false

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

            atlases["ailments"] = TextureAtlas(Gdx.files.internal("ailments.atlas"))
            listOf("wind").forEach {
                sounds[it] = Gdx.audio.newSound(Gdx.files.internal("sounds/$it.ogg"))
            }
            battleResources.figures.forEach { figureName ->
                gdxModel.figures.firstOrNull { it.name == figureName }?.let {
                    loadResources(it)
                }
            }

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
        }
        gdxFigure.dependencies?.let { dependencies ->
            dependencies.forEach { dependencyFigure ->
                gdxModel.figures.firstOrNull { it.name == dependencyFigure }?.let { loadResources(it) }
            }
        }
        gdxFigure.attacks.forEach {
            it.sound?.let { sounds[it] = Gdx.audio.newSound(Gdx.files.internal("sounds/${it}.ogg")) }
            it.effect?.sound?.let { sounds[it] = Gdx.audio.newSound(Gdx.files.internal("sounds/${it}.ogg")) }
        }
        gdxFigure.poses.forEach {
            it.sound?.let { sounds[it] = Gdx.audio.newSound(Gdx.files.internal("sounds/${it}.ogg")) }
        }
    }

    private fun usePotion(flask: FlaskItemFullInfoDto) {
        KtxAsync.launch {
            gameActor.refreshAlchemy()
        }
    }

    private suspend fun claimRewards(): LocationCompleteResponseDto {
        game.accountService.refreshPersonages()
        return actService.markLocationComplete(actId, locationId, difficulty, personage.id)
    }

    private fun listenEvents() {
        KtxAsync.launch {
            game.api.connectBattle(battleId, swipeFlow) {
                gameActor.processAction(it)
                battleController.processEvent(it)
                when (it) {
                    is BattleEvent.VictoryEvent -> onGameEnded(true)
                    is BattleEvent.DefeatEvent -> onGameEnded(false)
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

//    private fun produceTutorials(): List<AbilityTrigger> {
//        val result = mutableListOf<AbilityTrigger>()
//        if (actId == "act_0" && locationId == 0 && TutorialKeys.tutorialsEnabled && storage.get(TutorialKeys.ACT1_FIRST_BATTLE_INTRO_SHOWN)?.toBoolean() != true) { result.add(FirstBattleTutorial(this, game)) }
//        if (actId == "act_0" && locationId == 2 && TutorialKeys.tutorialsEnabled && storage.get(TutorialKeys.ACT1_L3_TALK)?.toBoolean() != true) { result.add(Act1L3Talk(this, game)) }
//        if (actId == "act_0" && locationId == 3 && TutorialKeys.tutorialsEnabled && storage.get(TutorialKeys.ACT1_L4_TALK)?.toBoolean() != true) { result.add(Act1L4Talk(this, game)) }
//        if (actId == "act_0" && locationId == 4 && TutorialKeys.tutorialsEnabled && storage.get(TutorialKeys.ACT1_L5_TALK)?.toBoolean() != true) { result.add(Act1L5Talk(this, game)) }
//        if (actId == "act_0" && locationId == 6 && TutorialKeys.tutorialsEnabled && storage.get(TutorialKeys.ACT1_L7_TALK)?.toBoolean() != true) { result.add(Act1L7Talk(this, game)) }
//        if (actId == "act_0" && locationId == 7 && TutorialKeys.tutorialsEnabled && storage.get(TutorialKeys.ACT1_L8_TALK)?.toBoolean() != true) { result.add(Act1L8Talk(this, game)) }
//        if (actId == "act_0" && locationId == 8 && TutorialKeys.tutorialsEnabled && storage.get(TutorialKeys.ACT1_L9_TALK)?.toBoolean() != true) { result.add(Act1L9Talk(this, game)) }
//        if (actId == "act_0" && locationId == 10 && TutorialKeys.tutorialsEnabled && storage.get(TutorialKeys.ACT1_L11_TALK)?.toBoolean() != true) { result.add(Act1L11Talk(this, game)) }
//        if (actId == "act_0" && locationId == 13 && TutorialKeys.tutorialsEnabled && storage.get(TutorialKeys.ACT1_L14_TALK)?.toBoolean() != true) { result.add(Act1L14Talk(this, game)) }
//        return result
//    }

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