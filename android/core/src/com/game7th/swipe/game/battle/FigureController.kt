package com.game7th.swipe.game.battle

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.game7th.swipe.game.GameContextWrapper
import com.game7th.swipe.game.battle.model.FigureGdxModel
import com.game7th.swipe.game.battle.model.mapNameToFigure

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
        private val scale: Float,
        private val flipped: Boolean
) : ElementController(context) {

    var timePassed = 0f
    var timePoseStarted = 0f

    var atlas: TextureAtlas = context.atlases[figureModel.atlas]!!
    val allTextures = filterAtlas(atlas, figureModel.name.mapNameToFigure()).toList()
    var animation: Animation<TextureRegion>? = null

    lateinit var pose: FigurePose
    private val flipMultiplier = if (flipped) -1 else 1

    init {
        switchPose(FigurePose.POSE_IDLE)
        println("$flipMultiplier FLIP")
    }

    override fun render(batch: SpriteBatch, delta: Float) {
        timePassed += delta

        animation?.let { animation ->
            batch.draw(animation.getKeyFrame(timePassed, true),
                    x - 256f * scale * flipMultiplier,
                    y,
                    512f * scale * flipMultiplier,
                    512f * scale)
        }

    }

    private fun switchPose(pose: FigurePose) {
        timePoseStarted = timePassed
        this.pose = pose
        val pose = figureModel.poses.firstOrNull { it.name == pose.poseName }
        this.animation = Animation(FRAME_DURATION, Array(allTextures.subList((pose?.start ?: 1) - 1, (pose?.end ?: 1)).toTypedArray()))
    }

    companion object {
        const val FRAME_DURATION = 1/30f
    }
}