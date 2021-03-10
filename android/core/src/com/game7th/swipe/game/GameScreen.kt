package com.game7th.swipe.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.badlogic.gdx.utils.viewport.Viewport
import com.game7th.battle.BattleConfig
import com.game7th.battle.PersonageConfig
import com.game7th.battle.SwipeBattle
import com.game7th.battle.event.BattleEvent
import com.game7th.metagame.account.PersonageAttributeStats
import com.game7th.metagame.account.PersonageData
import com.game7th.metagame.campaign.ActsService
import com.game7th.swipe.SwipeGameGdx
import com.game7th.swipe.campaign.ActScreen
import com.game7th.swipe.game.actors.GameActor
import com.game7th.swipe.game.battle.BattleController
import com.game7th.swipe.game.battle.model.GdxModel
import com.game7th.swipe.gestures.SimpleDirectionGestureDetector
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import ktx.async.KtxAsync

class GameScreen(private val game: SwipeGameGdx,
                 private val actId: Int,
                 private val locationId: Int,
                 private val difficulty: Int,
                 private val personage: PersonageData,
                 private val actService: ActsService
) : Screen {

    lateinit var stage: Stage
    lateinit var viewport: Viewport

    lateinit var battleController: BattleController

    lateinit var batch: SpriteBatch

    lateinit var processor: SimpleDirectionGestureDetector
    lateinit var battle: SwipeBattle
    val handler = CoroutineExceptionHandler { _, exception ->
        exception.printStackTrace()
    }
    lateinit var config: BattleConfig

    lateinit var gameActor: GameActor
    val atlases = mutableMapOf<String, TextureAtlas>()
    val sounds = mutableMapOf<String, Sound>()

    lateinit var backgroundMusic: Music

    override fun show() {
        viewport = ExtendViewport(480f, 720f, 480f, 2000f)
        stage = Stage(viewport)

        batch = SpriteBatch()

        processor = SimpleDirectionGestureDetector(object : SimpleDirectionGestureDetector.DirectionListener {
            override fun onLeft() {
                KtxAsync.launch {
                    battle.processSwipe(-1, 0)
                }
            }

            override fun onRight() {
                KtxAsync.launch {
                    battle.processSwipe(1, 0)
                }
            }

            override fun onUp() {
                KtxAsync.launch {
                    battle.processSwipe(0, -1)
                }
            }

            override fun onDown() {
                KtxAsync.launch {
                    battle.processSwipe(0, 1)
                }
            }
        })
        game.multiplexer.addProcessor(0, processor)

        config = BattleConfig(
                personages = listOf(
                        PersonageConfig(personage.unit, personage.level, personage.stats, game.context.balance.produceGearStats(personage))
                ),
                waves = actService.getActConfig(actId).findNode(locationId)?.waves?.map {
                    it.map { PersonageConfig(it.unitType, it.level + (difficulty-1) * 3,  PersonageAttributeStats(0,0,0), null) }
                } ?: emptyList()
        )

        battle = SwipeBattle(game.context.balance)
        initializeBattle()
        listenEvents()

        gameActor = GameActor(
                game.context, ::processTileDoubleTap, this::claimRewards) { _ ->
            game.switchScreen(ActScreen(game, game.actService, actId, game.screenContext, game.storage))
        }

        stage.addActor(gameActor)

        game.multiplexer.addProcessor(stage)

        val gdxModel = Gson().fromJson<GdxModel>(Gdx.files.internal("figures.json").readString(), GdxModel::class.java)

        atlases["ailments"] = TextureAtlas(Gdx.files.internal("ailments.atlas"))
        listOf("wind").forEach {
            sounds[it] = Gdx.audio.newSound(Gdx.files.internal("sounds/$it.ogg"))
        }
        config.personages.forEach { personageConfig ->
            atlases[personageConfig.name.getSkin()] = TextureAtlas(Gdx.files.internal("${personageConfig.name.getSkin()}.atlas"))
            gdxModel.figures.firstOrNull { it.name == personageConfig.name.getSkin() }?.let {
                it.attacks.forEach {
                    it.sound?.let { sounds[it] = Gdx.audio.newSound(Gdx.files.internal("sounds/${it}.ogg")) }
                    it.effect?.sound?.let { sounds[it] = Gdx.audio.newSound(Gdx.files.internal("sounds/${it}.ogg"))  }
                }
                it.poses.forEach {
                    it.sound?.let { sounds[it] = Gdx.audio.newSound(Gdx.files.internal("sounds/${it}.ogg")) }
                }
            }
        }
        config.waves.forEach { it.forEach { npc ->
            if (!atlases.containsKey(npc.name.getSkin())) {
                atlases[npc.name.getSkin()] = TextureAtlas(Gdx.files.internal("${npc.name.getSkin()}.atlas"))
            }
            gdxModel.figures.firstOrNull { it.name == npc.name.getSkin() }?.let {
                it.attacks.forEach {
                    it.sound?.let { sounds[it] = Gdx.audio.newSound(Gdx.files.internal("sounds/${it}.ogg")) }
                    it.effect?.sound?.let { sounds[it] = Gdx.audio.newSound(Gdx.files.internal("sounds/${it}.ogg"))  }
                }
                it.poses.forEach {
                    it.sound?.let { sounds[it] = Gdx.audio.newSound(Gdx.files.internal("sounds/${it}.ogg")) }
                }
            }
        }}

        val scale = game.context.scale
        battleController = BattleController(GameContextWrapper(
                gameContext = game.context,
                scale = scale,
                gdxModel = gdxModel,
                width = Gdx.graphics.width.toFloat(),
                height = Gdx.graphics.height.toFloat(),
                atlases = atlases
        ), Gdx.graphics.width.toFloat(), sounds) {
            if (it is BattleEvent.VictoryEvent) {
                val experience = config.waves.sumBy {
                    it.sumBy { it.level * 50 }
                }
                val expResult = game.accountService.addPersonageExperience(personage.id, experience)
                gameActor.showVictory(expResult)
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
    }

    private fun claimRewards() = actService.markLocationComplete(actId, locationId, difficulty)

    private fun processTileDoubleTap(id: Int) {
        KtxAsync.launch {
            battle.attemptActivateTile(id)
        }
    }

    private fun initializeBattle() {
        KtxAsync.launch {
            battle.initialize(config)
        }
    }

    private fun listenEvents() {
        KtxAsync.launch(handler) {
            for (event in battle.events) {
                gameActor.processAction(event)
                battleController.enqueueEvent(event)
            }
        }
    }

    override fun render(delta: Float) {
        batch.begin()
        battleController.act(batch, delta)
        batch.end()

        stage.act(Gdx.graphics.deltaTime)
        stage.draw()
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
}