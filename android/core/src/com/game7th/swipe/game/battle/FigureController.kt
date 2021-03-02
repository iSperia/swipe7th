package com.game7th.swipe.game.battle

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.game7th.swipe.game.GameContextWrapper
import com.game7th.swipe.game.battle.model.BattleControllerEvent
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
        id: Int,
        val battle: BattleController,
        val figureModel: FigureGdxModel,
        val originX: Float,
        val originY: Float,
        private val scale: Float,
        val flipped: Boolean
) : ElementController(context, id) {

    var x = originX
    var y = originY

    var timePassed = 0f
    var timePoseStarted = 0f

    var atlas: TextureAtlas = context.atlases[figureModel.atlas]!!
    val allTextures = filterAtlas(atlas, figureModel.name.mapNameToFigure()).toList()
    var animation: Animation<TextureRegion>? = null

    var isDead = false

    lateinit var pose: FigurePose
    private val flipMultiplier = if (flipped) -1 else 1

    var oldIndex = -1

    init {
        switchPose(FigurePose.POSE_IDLE)
    }

    override fun render(batch: SpriteBatch, delta: Float) {
        timePassed += delta * battle.timeScale()

        animation?.let { animation ->
            if (pose != FigurePose.POSE_DEATH || !animation.isAnimationFinished(timePassed - timePoseStarted)) {
                batch.draw(animation.getKeyFrame(timePassed - timePoseStarted, true),
                        x - 256f * scale * flipMultiplier,
                        y,
                        512f * scale * flipMultiplier,
                        512f * scale)
            } else if (pose == FigurePose.POSE_DEATH) {
                if (flipped) {
                    battle.removeController(this)
                } else {
                    batch.draw(animation.keyFrames.last(),
                            x - 256f * scale * flipMultiplier,
                            y,
                            512f * scale * flipMultiplier,
                            512f * scale)
                }
            }

            val index = animation.getKeyFrameIndex(timePassed - timePoseStarted)
            if (index != oldIndex) {
                val pose = figureModel.poses.first { pose.poseName == it.name }

                (oldIndex + 1..index).forEach {
                    val hasTrigger = pose.triggers?.contains(it) == true
                    if (hasTrigger) {
                        battle.propagate(BattleControllerEvent.FigurePoseFrameIndexEvent(id, index))
                    }
                }

                oldIndex = index
            }
            if (animation.isAnimationFinished(timePassed - timePoseStarted) && animation.playMode == Animation.PlayMode.NORMAL
                    && pose != FigurePose.POSE_DEATH) {
                switchPose(FigurePose.POSE_IDLE)
            }
        }

    }

    override fun handle(event: BattleControllerEvent) {
    }

    fun switchPose(pose: FigurePose) {
        if (isDead) return
        timePoseStarted = timePassed
        this.pose = pose
        val pose = figureModel.poses.firstOrNull { it.name == pose.poseName }
        val playMode = when (this.pose) {
            FigurePose.POSE_IDLE -> Animation.PlayMode.LOOP
            FigurePose.POSE_ATTACK -> Animation.PlayMode.NORMAL
            FigurePose.POSE_DAMAGE -> Animation.PlayMode.NORMAL
            FigurePose.POSE_DEATH -> Animation.PlayMode.NORMAL
        }
        this.animation = Animation(FRAME_DURATION, Array(allTextures.subList((pose?.start ?: 1) - 1, (pose?.end ?: 1)).toTypedArray()), playMode)
    }

    companion object {
        const val FRAME_DURATION = 1/30f
    }
}