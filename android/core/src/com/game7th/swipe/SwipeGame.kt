package com.game7th.swipe

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.RotateByAction
import com.badlogic.gdx.scenes.scene2d.actions.ScaleToAction
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.utils.viewport.FitViewport
import ktx.actors.repeatForever

class SwipeGame : ApplicationListener {
    lateinit var stage: Stage
    lateinit var viewport: FitViewport

    lateinit var atlas: TextureAtlas

    override fun create() {
        viewport = FitViewport(480f, 720f)
        stage = Stage(viewport)

        atlas = TextureAtlas(Gdx.files.internal("pack_0.atlas"))

        val actor = Group().apply {
            addActor(Image(atlas.findRegion("tile_bg", 1)))
            addActor(Image(atlas.findRegion("skill_tile_holy_strike")))

            (1..6).forEach {
                addActor(Image(atlas.findRegion("tile_fg_empire")).apply {
                    originX = 16f
                    originY = 16f
                    rotation = it * 360 / 6f
                    addAction(RotateByAction().apply {
                        duration = 2f
                        amount = -360f
                    }.repeatForever())
                })
            }


            originX = 32f
            originY = 32f
            x = 128f + 32f
            y = 192f + 32f
            scaleX = 2.2f
            scaleY = 2.2f
            zIndex = 10

            addAction(SequenceAction(
                    ScaleToAction().apply {
                        setScale(1.8f)
                        duration = 0.5f
                    },
                    ScaleToAction().apply {
                        setScale(2.2f)
                        duration = 0.5f
                    }).repeatForever())
        }


        val actor2 = Group().apply {
            addActor(Image(atlas.findRegion("tile_bg", 1)))
            addActor(Image(atlas.findRegion("skill_tile_holy_strike")))

            x = 256f
            y = 192f - 64f
            scaleX = 2f
            scaleY = 2f
            zIndex = 10
        }

        val actor3 = Group().apply {
            addActor(Image(atlas.findRegion("tile_bg", 0)))
            addActor(Image(atlas.findRegion("skill_tile_holy_strike")))

            x = 64f
            y = 192f - 64f
            scaleX = 2f
            scaleY = 2f
            zIndex = 10
        }

        (1..6).forEach { i ->
            (1..6).forEach { j ->
                stage.addActor(Image(atlas.findRegion("tile_bg_grey")).apply {
                    x = 64f * j
                    y = 64f * (6 - i)
                    scaleX = 2f
                    scaleY = 2f
                    zIndex = 100
                })
            }
        }

        stage.addActor(actor)
        stage.addActor(actor2)
        stage.addActor(actor3)
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
}