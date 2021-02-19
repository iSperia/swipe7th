package com.game7th.swipe.game.battle

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.game7th.swipe.game.GameContextWrapper
import com.game7th.swipe.game.battle.model.FigureGdxModel

/**
 * GDX graphics figure controller with poses
 */
class FigureController(
        context: GameContextWrapper,
        private val figureModel: FigureGdxModel,
        private val x: Float,
        private val y: Float
) : ElementController(context) {

    var timePassed = 0f

    val textures = filterAtlas(context.atlases["personage_gladiator"]!!, "body_gladiator")
    val idleTextures = textures.toList().subList(27, 51).toTypedArray().let { Array(it) }
    val animation = Animation<TextureRegion>(1/15f, idleTextures, Animation.PlayMode.LOOP)

    override fun render(batch: SpriteBatch, delta: Float) {
        timePassed += delta

        batch.draw(animation.getKeyFrame(timePassed, true),
                (x - 256f) * context.gameContext.scale,
                y * context.gameContext.scale,
        512f * context.gameContext.scale,
        512f * context.gameContext.scale)
    }
}