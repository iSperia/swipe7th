package com.game7th.swipe

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.FitViewport
import com.game7th.battle.balance.SwipeBalance
import com.game7th.swipe.constructor.ConstructorView
import com.game7th.swipe.game.actors.GameView
import com.google.gson.Gson
import ktx.async.KtxAsync

class SwipeGameGdx : ApplicationListener {
    lateinit var stage: Stage
    lateinit var viewport: FitViewport

    val multiplexer = InputMultiplexer()

    override fun create() {
        KtxAsync.initiate()

        viewport = FitViewport(VP_WIDTH, VP_HEIGHT)
        stage = Stage(viewport)

        val atlas = TextureAtlas(Gdx.files.internal("pack_0.atlas"))
        val font = BitmapFont()
        val balanceFile = Gdx.files.internal("balance.json")
        val balanceText = balanceFile.readString()
        val balance = Gson().fromJson<SwipeBalance>(balanceText, SwipeBalance::class.java)
        val context = GdxGameContext(atlas, font, balance)

        Gdx.input.inputProcessor = multiplexer
        multiplexer.addProcessor(stage)

        val constructor = ConstructorView(context)
        stage.addActor(constructor)
    }

    override fun render() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        stage.act(Gdx.graphics.deltaTime)
        stage.draw()
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun dispose() {

    }

    override fun resize(width: Int, height: Int) {
    }

    companion object {
        const val VP_WIDTH = 480f
        const val VP_HEIGHT = 720f
    }
}