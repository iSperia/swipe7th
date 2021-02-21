package com.game7th.swipe.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.badlogic.gdx.utils.viewport.Viewport
import com.game7th.battle.BattleConfig
import com.game7th.battle.PersonageConfig
import com.game7th.battle.SwipeBattle
import com.game7th.metagame.campaign.ActsService
import com.game7th.metagame.unit.UnitType
import com.game7th.swipe.SwipeGameGdx
import com.game7th.swipe.campaign.ActScreen
import com.game7th.swipe.game.actors.GameActor
import com.game7th.swipe.game.battle.BattleController
import com.game7th.swipe.gestures.SimpleDirectionGestureDetector
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import ktx.async.KtxAsync

class GameScreen(private val game: SwipeGameGdx,
                 private val actId: Int,
                 private val locationId: Int,
                 private val difficulty: Int,
                 private val personageSquadId: Int,
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
                        PersonageConfig(UnitType.POISON_ARCHER, personageSquadId + 1),
                        PersonageConfig(UnitType.MACHINE_GUNNER, personageSquadId + 1),
                        PersonageConfig(UnitType.GLADIATOR, personageSquadId + 1)
                ),
                waves = actService.getActConfig(actId).findNode(locationId)?.waves?.map {
                    it.map { PersonageConfig(it.unitType, it.level) }
                } ?: emptyList()
        )

        battle = SwipeBattle(game.context.balance)
        initializeBattle()
        listenEvents()

        gameActor = GameActor(
                game.context, ::processTileDoubleTap) { victory ->
            if (victory) {
                actService.markLocationComplete(actId, locationId, difficulty)
            }
            game.screen = ActScreen(game, game.actService, actId, game.screenContext)
        }

        stage.addActor(gameActor)

        game.multiplexer.addProcessor(stage)

        battleController = BattleController(GameContextWrapper(
                gameContext = game.context,
                width = Gdx.graphics.width.toFloat(),
                height = Gdx.graphics.height.toFloat(),
                atlases = mapOf("personage_gladiator" to TextureAtlas(Gdx.files.internal("personage_gladiator.atlas")))
        ), Gdx.graphics.width.toFloat())
    }

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
    }

    override fun dispose() {
        game.multiplexer.removeProcessor(processor)
    }
}