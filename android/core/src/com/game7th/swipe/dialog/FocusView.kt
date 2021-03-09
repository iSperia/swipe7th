package com.game7th.swipe.dialog

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20.*
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.game7th.swipe.ScreenContext

class FocusView(
        private val context: ScreenContext,
        private val rect: Rectangle,
        private val text: String
) : Group() {

    private val modalPanel = Image(context.uiAtlas.findRegion("panel_modal")).apply {
        width = Gdx.graphics.width.toFloat()
        height = Gdx.graphics.height.toFloat()
    }

    private val edge = Image(context.uiAtlas.findRegion("panel_edge")).apply {
        x = rect.x - context.scale
        y = rect.y - context.scale
        width = rect.width + 2 * context.scale
        height = rect.height + 2 * context.scale
        touchable = Touchable.disabled
    }

    private val mask = TextureRegionDrawable(context.uiAtlas.findRegion("panel_modal"))

    private val textLabel = Label(text, Label.LabelStyle(context.font, Color.WHITE)).apply {
        x = rect.x
        y = rect.height + 12f * context.scale + rect.y
        width = 480f * context.scale - rect.x
        height = 60f
        setAlignment(Align.bottomLeft)
        wrap = true
        touchable = Touchable.disabled
        setFontScale(200f/3f/36f)
    }

    init {
        addActor(modalPanel)
        addActor(edge)
        addActor(textLabel)

        modalPanel.addListener { event ->
            when (event) {
                is InputEvent -> {
                    !rect.contains(event.stageX, event.stageY)
                }
                else -> false
            }
        }
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        Gdx.gl.glClear(GL_STENCIL_BUFFER_BIT)
        batch.end()
//disable color mask
        Gdx.gl.glColorMask(false, false, false, false)
        Gdx.gl.glDepthMask(false)
//enable the stencil
        Gdx.gl.glEnable(GL_STENCIL_TEST)
        Gdx.gl.glStencilFunc(GL_ALWAYS, 0x1, 0xffffffff.toInt())
        Gdx.gl.glStencilOp(GL_REPLACE, GL_REPLACE, GL_REPLACE)

        batch.begin()
//draw the mask
        mask.draw(batch, rect.x, rect.y, rect.width, rect.height)

        batch.end()
        batch.begin()

//enable color mask
        Gdx.gl.glColorMask(true, true, true, true)
        Gdx.gl.glDepthMask(true)
//just draw where outside of the mask
        Gdx.gl.glStencilFunc(GL_NOTEQUAL, 0x1, 0xffffffff.toInt())
        Gdx.gl.glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP)
        super.draw(batch, parentAlpha)
//disable the stencil
        Gdx.gl.glDisable(GL_STENCIL_TEST)
    }
}