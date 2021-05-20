package com.game7th.swipe.game.battle

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.game7th.swipe.game.GameContextWrapper
import com.game7th.swipe.game.battle.model.EffectGdxModel

class EffectController(
        context: GameContextWrapper,
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

    init {
        this.animation = Animation(FigureController.FRAME_DURATION, Array(allTextures.toTypedArray()), Animation.PlayMode.NORMAL)
    }

    override fun render(batch: SpriteBatch, delta: Float) {
        timePassed += delta * battle.timeScale()

        if (!animation.isAnimationFinished(timePassed)) {
            batch.draw(animation.getKeyFrame(timePassed, true),
                    targetFigure.originX + (effect.anchor_x?.toFloat() ?: (effect.width / 2f)) * battle.scale * flipMultiplier,
                    targetFigure.originY + (effect.anchor_y ?: 0) * battle.scale,
                    -effect.width * battle.scale * flipMultiplier,
                    effect.height * battle.scale)
        }

        if (animation.isAnimationFinished(timePassed)) {
            battle.removeController(this)
        }
    }

    override fun dispose() {
    }
}