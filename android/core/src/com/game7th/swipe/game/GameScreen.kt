package com.game7th.swipe.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.FitViewport
import com.game7th.swipe.SwipeGameGdx

class GameScreen(private val game: SwipeGameGdx) : Screen {

    lateinit var stage: Stage
    lateinit var viewport: FitViewport

    override fun show() {
        viewport = FitViewport(VP_WIDTH, VP_HEIGHT)
        stage = Stage(viewport)
        game.multiplexer.addProcessor(stage)
    }

    override fun render(delta: Float) {
        stage.act(Gdx.graphics.deltaTime)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        TODO("Not yet implemented")
    }

    override fun pause() {
        TODO("Not yet implemented")
    }

    override fun resume() {
        TODO("Not yet implemented")
    }

    override fun hide() {
        TODO("Not yet implemented")
    }

    override fun dispose() {
        TODO("Not yet implemented")
    }
    companion object {
        const val VP_WIDTH = 480f
        const val VP_HEIGHT = 720f
    }
}