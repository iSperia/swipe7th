package com.game7th.swipe.game.actors

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.game7th.swipe.game.BattleContext

class ScaleActor(
        private val context: BattleContext,
        private val textureName: String,
        private val numLines: Int
) : Group() {

    private val background = Image(context.battleAtlas.findRegion("scale_bg")).apply {
        width = 16f * context.scale
        height = 271f * context.scale
    }

    private val flash = Image(context.battleAtlas.findRegion("scale_flash_pink")).apply {
        width = background.width
        height = background.height
    }

    private val filledEffect = Image(context.battleAtlas.findRegion("scale_filled_effect")).apply {
        width = 16f * context.scale
        height = 271f * context.scale
        isVisible = false
    }

    private val whiteLine = TextureRegionDrawable(context.battleAtlas.findRegion("white_line"))

    private val scaleTexture = context.battleAtlas.findRegion(textureName)
    private val sx = (background.width - 6f * context.scale) / 2f
    private val sy = (background.height - 244f * context.scale) / 2f
    private val lineDelta = 244f * context.scale / numLines

    private var progress: Float = 0f
    private var displayedProgress: Float = 0f

    private var showFilledEffectLeft = 0f

    init {
        addActor(background)
        addActor(flash)
        addActor(filledEffect)
    }

    fun applyProgress(wisdomProgress: Int) {
        val oldProgress = progress
        progress = wisdomProgress / 100f
        if (oldProgress > 0.2f && progress == 0f && numLines == 0) {
            showFilledEffectLeft = 0.1f
            filledEffect.isVisible = true
            displayedProgress = 0f
        }
    }

    override fun act(delta: Float) {
        super.act(delta)
        if (showFilledEffectLeft > 0f) {
            showFilledEffectLeft -= delta
            if (showFilledEffectLeft <= 0f) {
                filledEffect.isVisible = false
            }
        }
        if (displayedProgress != progress) {
            displayedProgress += (progress - displayedProgress) / 15
        }
    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        applyTransform(batch, computeTransform())
        batch?.draw(scaleTexture.texture, sx, sy, 6f * context.scale, 244f * context.scale * displayedProgress, scaleTexture.u, scaleTexture.v2, scaleTexture.u2, scaleTexture.v2 + (scaleTexture.v - scaleTexture.v2) * displayedProgress)
        if (numLines > 0) {
            (0 until numLines).forEach { index ->
                whiteLine.draw(batch, sx, sy + index * lineDelta, 6f * context.scale, context.scale)
            }
        }
        resetTransform(batch)
    }
}