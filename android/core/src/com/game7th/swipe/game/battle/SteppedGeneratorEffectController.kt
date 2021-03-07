@file:Suppress("NAME_SHADOWING")

package com.game7th.swipe.game.battle

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.game7th.swipe.game.GameContextWrapper
import com.game7th.swipe.game.battle.model.BattleControllerEvent
import com.game7th.swipe.game.battle.model.EffectGdxModel
import kotlin.random.Random

class SteppedGeneratorEffectController(
        context: GameContextWrapper,
        id: Int,
        private val battle: BattleController,
        private val x: Float,
        private val y: Float,
        private val targetX: Float,
        private val triggers: List<Float>,
        private val model: EffectGdxModel
) : ElementController(context, id) {
    private val animations = mutableListOf<Pair<Float, Animation<TextureRegion>>>()

    private var passedTime = 0f

    private val dx = -model.width * battle.scale / 2f

    private var anyStarted = false

    private val textures = filterAtlas(context.atlases[model.atlas]!!, model.name)

    var nextTriggerIndex = 0

    override fun render(batch: SpriteBatch, delta: Float) {
        passedTime += delta * battle.timeScale()
        val index = (passedTime / model.time).toInt() + 1
        if (animations.size < index) {
            val nextX = x + index * model.step!! * battle.scale
            if (nextX < targetX) {
                val animation = Animation<TextureRegion>(model.time, textures)
                animation.frameDuration = 1/60f + 1/30f * Random.nextFloat()
                animation.playMode = Animation.PlayMode.NORMAL
                animations.add(Pair(passedTime, animation))

                if (triggers.size > nextTriggerIndex && triggers[nextTriggerIndex] < nextX) {
                    battle.propagate(BattleControllerEvent.SteppedGeneratorEvent(nextTriggerIndex))
                    nextTriggerIndex++
                }
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
                        x + index * model.step!! * battle.scale + dx,
                        y,
                        model.width * battle.scale,
                        model.height * battle.scale)
            }
        }

        if (!anyStarted) {
            animations.clear()
            battle.removeController(this)
        }
    }

    override fun handle(event: BattleControllerEvent) {
    }
}