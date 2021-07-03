package com.game7th.swipe.game.battle

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.game7th.swipe.game.BattleContext
import com.game7th.swipe.game.battle.model.EffectGdxModel

class EffectController(
        context: BattleContext,
        battle: BattleController,
        id: Int,
        private val targetFigure: FigureController,
        private val effect: EffectGdxModel
) : ElementController(context, battle, id) {

    private val flipMultiplier = if (targetFigure.flipped) 1f else -1f

    var timePassed = 0f
    var atlas: TextureAtlas = context.atlases[effect.atlas]!!
    val allTextures = filterAtlas(atlas, effect.name).toList()
    val animation: Animation<TextureRegion>
    val scale = effect.scale ?: 1f

    init {
        this.animation = Animation(FigureController.FRAME_DURATION, Array(allTextures.toTypedArray()), Animation.PlayMode.NORMAL)
    }

    override fun render(batch: SpriteBatch, delta: Float) {
        timePassed += delta * battle.timeScale()

        if (!animation.isAnimationFinished(timePassed)) {
            batch.draw(animation.getKeyFrame(timePassed, true),
                    targetFigure.originX + (effect.anchor_x?.toFloat() ?: (effect.width / 2f)) * battle.scale * scale * flipMultiplier,
                    targetFigure.originY + (effect.anchor_y ?: 0) * battle.scale * scale,
                    -effect.width * battle.scale * flipMultiplier * scale,
                    effect.height * battle.scale * scale)
        }

        if (animation.isAnimationFinished(timePassed)) {
            battle.removeController(this)
        }
    }

    override fun dispose() {
    }
}