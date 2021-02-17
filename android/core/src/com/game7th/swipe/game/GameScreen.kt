package com.game7th.swipe.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.FitViewport
import com.game7th.battle.BattleConfig
import com.game7th.battle.PersonageConfig
import com.game7th.metagame.campaign.ActsService
import com.game7th.metagame.unit.UnitType
import com.game7th.swipe.SwipeGameGdx
import com.game7th.swipe.campaign.ActScreen
import com.game7th.swipe.game.actors.GameView

class GameScreen(private val game: SwipeGameGdx,
                 private val actId: Int,
                 private val locationId: Int,
                 private val difficulty: Int,
                 private val personageSquadId: Int,
                 private val actService: ActsService
    ) : Screen {

    lateinit var stage: Stage
    lateinit var viewport: FitViewport

    lateinit var gameView: GameView

    override fun show() {
        viewport = FitViewport(VP_WIDTH, VP_HEIGHT)
        stage = Stage(viewport)

        gameView = GameView(game.context, game.multiplexer, BattleConfig(
                personages = listOf(
                        PersonageConfig(UnitType.POISON_ARCHER, personageSquadId + 1),
                        PersonageConfig(UnitType.MACHINE_GUNNER, personageSquadId + 1),
                        PersonageConfig(UnitType.GLADIATOR, personageSquadId + 1)
                ),
                waves = actService.getActConfig(actId).findNode(locationId)?.waves?.map {
                    it.map { PersonageConfig(it.unitType, it.level) }
                } ?: emptyList()
        )) { victory ->
            if (victory) {
                actService.markLocationComplete(actId, locationId, difficulty)
            }
            game.screen = ActScreen(game, game.actService, actId, game.screenContext)
        }

        stage.addActor(gameView)

        game.multiplexer.addProcessor(stage)
    }

    override fun render(delta: Float) {
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
    }

    override fun dispose() {
    }

    companion object {
        const val VP_WIDTH = 480f
        const val VP_HEIGHT = 720f
    }
}