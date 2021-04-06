@file:Suppress("NAME_SHADOWING")

package com.game7th.swipe.game.battle

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.game7th.battle.personage.PersonageViewModel
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
        battle: BattleController,
        id: Int,
        y: Float,
        val figureModel: FigureGdxModel,
        var viewModel: PersonageViewModel,
        val player: (String) -> Unit
) : ElementController(context, battle, id) {

    var originX: Float = 0f

    val originY: Float = y

    var x = originX
    var y = originY

    var timePassed = 0f
    var timePoseStarted = 0f
    var timeMoveStarted = 0f
    var timeMoveFinished = 0f

    var fromX = x
    var fromY = y
    var targetX = x
    var targetY = y

    val frozenTexture = context.gameContext.battleAtlas.findRegion("ailment_frozen")

    var atlas: TextureAtlas = context.atlases[figureModel.atlas]!!
    val allTextures = filterAtlas(atlas, figureModel.body).toList()
    var animation: Animation<TextureRegion>? = null

    var isDead = false

    lateinit var pose: FigurePose
    val flipped = viewModel.team > 0
    private val flipMultiplier = if (flipped) -1 else 1

    var position: Int = 0

    init {
        switchPose(FigurePose.POSE_IDLE)
    }

    override fun render(batch: SpriteBatch, delta: Float) {
        timePassed += delta * battle.timeScale()

        if (targetX != x || targetY != y) {
            val percent = (timePassed - timeMoveStarted) / (timeMoveFinished - timeMoveStarted)
            if (percent >= 1f) {
                x = targetX
                y = targetY
            } else {
                x = fromX + (targetX - fromX) * percent
                y = fromY + (targetY - fromY) * percent
            }
        }

        val bodyScale = battle.scale * if (figureModel.scale > 0f) figureModel.scale else 1f
        animation?.let { animation ->
            if (pose != FigurePose.POSE_DEATH || !animation.isAnimationFinished(timePassed - timePoseStarted)) {
                batch.draw(animation.getKeyFrame(timePassed - timePoseStarted, true),
                        x - 256f * bodyScale * flipMultiplier,
                        y,
                        512f * bodyScale * flipMultiplier,
                        512f * bodyScale)
            } else if (pose == FigurePose.POSE_DEATH) {
                if (flipped) {
                    battle.removeController(this)
                } else {
                    batch.draw(animation.keyFrames.last(),
                            x - 256f * bodyScale * flipMultiplier,
                            y,
                            512f * bodyScale * flipMultiplier,
                            512f * bodyScale)
                }
            }

            if (animation.isAnimationFinished(timePassed - timePoseStarted) && animation.playMode == Animation.PlayMode.NORMAL
                    && pose != FigurePose.POSE_DEATH && !isDead) {
                switchPose(FigurePose.POSE_IDLE)
            }
        }

        if (viewModel.stats.isFrozen) {
            batch.draw(frozenTexture, x - 64f * bodyScale * flipMultiplier, y, 128f * bodyScale * flipMultiplier, 64f * bodyScale)
        }

    }

    fun switchPose(pose: FigurePose) {
        if (isDead) return
        timePoseStarted = timePassed
        this.pose = pose
        val pose = figureModel.poses.firstOrNull { it.name == pose.poseName }
        pose?.sound?.let { player(it) }
        val playMode = when (this.pose) {
            FigurePose.POSE_IDLE -> Animation.PlayMode.LOOP
            FigurePose.POSE_ATTACK -> Animation.PlayMode.NORMAL
            FigurePose.POSE_DAMAGE -> Animation.PlayMode.NORMAL
            FigurePose.POSE_DEATH -> Animation.PlayMode.NORMAL
        }
        this.animation = Animation(FRAME_DURATION, Array(allTextures.subList((pose?.start ?: 1) - 1, (pose?.end ?: 1)).toTypedArray()), playMode)
    }

    fun move(targetX: Float, targetY: Float, duration: Float) {
        timeMoveStarted = timePassed
        timeMoveFinished = timeMoveStarted + duration
        this.targetX = targetX
        this.targetY = targetY
        this.fromX = x
        this.fromY = y
    }

    fun moveOrigin(targetX: Float, targetY: Float, duration: Float) {
        val needFastMove = originX > 0f

        originX = targetX

        if (needFastMove) {
            move(originX, targetY, duration)
        } else {
            x = originX
            fromX = originX
            this.targetX = originX
        }
    }

    companion object {
        const val FRAME_DURATION = 1/30f
    }
}