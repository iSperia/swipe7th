package com.game7th.swipe.game.battle

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.game7th.swipe.game.GameContextWrapper
import com.game7th.swipe.game.battle.model.EffectGdxModel
import kotlin.random.Random

class SteppedGeneratorEffectController(
        context: GameContextWrapper,
        private val x: Float,
        private val y: Float,
        private val targetX: Float,
        private val model: EffectGdxModel
) : ElementController(context) {
    private val animations = mutableListOf<Pair<Float, Animation<TextureRegion>>>()

    private var passedTime = 0f

    private val dx = -model.width * context.gameContext.scale / 2f

    private var anyStarted = false

    private val textures = filterAtlas(context.atlases[model.atlas]!!, model.name)

    override fun render(batch: SpriteBatch, delta: Float) {
        passedTime += delta
        val index = (passedTime / model.time).toInt() + 1
        if (animations.size < index) {
            val nextX = (animations.size + index * model.step!! + dx) * context.gameContext.scale
            if (nextX < targetX * context.gameContext.scale) {
                val animation = Animation<TextureRegion>(model.time, textures)
                animation.frameDuration = 1/60f + 1/30f * Random.nextFloat()
                animation.playMode = Animation.PlayMode.NORMAL
                animations.add(Pair(passedTime, animation))
            }
        }

        anyStarted = false
        animations.forEachIndexed { index, pair ->
            val passedTime = passedTime - pair.first
            if (!pair.second.isAnimationFinished(passedTime)) {
                anyStarted = true
                val texture = pair.second.getKeyFrame(passedTime)
                batch.draw(
                        texture,
                        (x + index * model.step!! + dx) * context.gameContext.scale,
                        y * context.gameContext.scale,
                        model.width * context.gameContext.scale,
                        model.height * context.gameContext.scale)
            }
        }

        if (!anyStarted) {
            animations.clear()
        }
    }
}