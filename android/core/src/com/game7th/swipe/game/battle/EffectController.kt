package com.game7th.swipe.game.battle

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.game7th.swipe.game.GameContextWrapper
import com.game7th.swipe.game.battle.model.BattleControllerEvent
import com.game7th.swipe.game.battle.model.EffectGdxModel

class EffectController(
        context: GameContextWrapper,
        id: Int,
        private val battle: BattleController,
        private val targetFigure: FigureController,
        private val effect: EffectGdxModel
) : ElementController(context, id) {

    private val flipMultiplier = if (targetFigure.flipped) 1f else -1f

    var timePassed = 0f
    var atlas: TextureAtlas = context.atlases[effect.atlas]!!
    val allTextures = filterAtlas(atlas, effect.name).toList()
    val animation: Animation<TextureRegion>

    init {
        battle.lock(1)
        this.animation = Animation(FigureController.FRAME_DURATION, Array(allTextures.toTypedArray()), Animation.PlayMode.NORMAL)
    }

    var oldIndex = -1

    override fun render(batch: SpriteBatch, delta: Float) {
        timePassed += delta * battle.timeScale()

        batch.draw(animation.getKeyFrame(timePassed, true),
                targetFigure.originX - effect.width * battle.scale * flipMultiplier / 2f,
                targetFigure.originY,
                effect.width * battle.scale * flipMultiplier,
                effect.height * battle.scale)

        val index = animation.getKeyFrameIndex(timePassed)
        if (index != oldIndex) {
            if (effect.trigger in (oldIndex + 1)..index) {
                battle.propagate(BattleControllerEvent.EffectTriggerEvent(id))
                battle.unlock()
            }
            oldIndex = index
        }

        if (animation.isAnimationFinished(timePassed)) {
            battle.removeController(this)
        }
    }

    override fun dispose() {
    }

    override fun handle(event: BattleControllerEvent) {
    }
}