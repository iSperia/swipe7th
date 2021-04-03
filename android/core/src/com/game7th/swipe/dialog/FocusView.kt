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
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.ScreenContext
import ktx.actors.onClick
import kotlin.math.max
import kotlin.math.min

enum class DismissStrategy {
    DISMISS_ON_INSIDE,
    DISMISS_ON_OUTSIDE,
    DISMISS_FORCED
}

class FocusView(
        private val context: GdxGameContext,
        private val rect: Rectangle,
        private val text: String,
        private val dismissStrategy: DismissStrategy,
        private val dismissCallback: (() -> Unit)?
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
        val w = min(480f * context.scale, max(200f * context.scale, 480f * context.scale - rect.x))
        val bottom = rect.y + rect.height + 60f > Gdx.graphics.height
        x = max(0f, min(rect.x, 480f * context.scale - w))
        y = if (bottom) {
            rect.y - 60f
        } else {
            rect.height + 12f * context.scale + rect.y
        }
        width = w
        height = 60f
        setAlignment(if (bottom) Align.topLeft else Align.bottomLeft)
        wrap = true
        setFontScale(context.scale * 100f/3f/36f)
        touchable = Touchable.disabled
    }

    init {
        addActor(modalPanel)
        addActor(edge)
        addActor(textLabel)

        modalPanel.addListener { event ->
            when (event) {
                is InputEvent -> {
                    if (rect.contains(event.stageX, event.stageY) && event.type == InputEvent.Type.exit && dismissStrategy != DismissStrategy.DISMISS_FORCED) {
                        dismissCallback?.let {
                            this@FocusView.remove()
                            it.invoke()
                        }
                    } else if (!rect.contains(event.stageX, event.stageY) && event.type == InputEvent.Type.exit && dismissStrategy == DismissStrategy.DISMISS_ON_OUTSIDE) {
                        dismissCallback?.let {
                            this@FocusView.remove()
                            it.invoke()
                        }
                    }
                    true
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

    fun forceDismiss() {
        if (dismissStrategy == DismissStrategy.DISMISS_FORCED) {
            dismissCallback?.let {
                it.invoke()
            }
        }
    }
}