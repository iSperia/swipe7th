package com.game7th.swipe.game.battle

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.game7th.swipe.game.GameContextWrapper
import com.game7th.swipe.game.battle.model.FigureGdxModel

enum class FigurePose(val poseName: String) {
    POSE_IDLE("idle"),
    POSE_ATTACK("attack"),
    POSE_DAMAGE("damage"),
    POSE_DEATH("death")
}

/**
 * GDX graphics figure controller with poses
 */
class FigureController(
        context: GameContextWrapper,
        private val figureModel: FigureGdxModel,
        private val x: Float,
        private val y: Float,
        private val scale: Float
) : ElementController(context) {

    var timePassed = 0f
    var timePoseStarted = 0f

    var atlas: TextureAtlas = context.atlases[figureModel.atlas]!!
    val allTextures = filterAtlas(atlas, figureModel.name).toList()
    var animation: Animation<TextureRegion>? = null

    lateinit var pose: FigurePose

    init {
        switchPose(FigurePose.POSE_IDLE)
    }

    override fun render(batch: SpriteBatch, delta: Float) {
        timePassed += delta

        animation?.let { animation ->
            batch.draw(animation.getKeyFrame(timePassed, true),
                    x - 256f * scale,
                    y,
                    512f * scale,
                    512f * scale)
        }

    }

    private fun switchPose(pose: FigurePose) {
        timePoseStarted = timePassed
        this.pose = pose
        val pose = figureModel.poses.firstOrNull { it.name == pose.poseName }
        this.animation = Animation(FRAME_DURATION, Array(allTextures.subList(pose?.startFrame ?: 1, pose?.endFrame ?: 1 + 1).toTypedArray()))
    }

    companion object {
        const val FRAME_DURATION = 1/30f
    }
}