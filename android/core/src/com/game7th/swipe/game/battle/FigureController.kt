@file:Suppress("NAME_SHADOWING")

package com.game7th.swipe.game.battle

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.*
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.utils.Array
import com.esotericsoftware.spine.*
import com.game7th.swipe.game.BattleContext
import com.game7th.swipe.game.battle.model.FigureGdxModel
import com.game7th.swipe.game.battle.model.GdxRenderType
import com.game7th.swipe.game.battle.model.PoseEffectGdxModel
import com.game7th.swipe.game.battle.model.PoseGdxModel
import com.game7th.swiped.api.battle.UnitViewModel

sealed class FigureRenderer {
    abstract fun render(figure: FigureController, batch: SpriteBatch, bodyScale: Float, delta: Float)
    abstract fun switchPose(figure: FigureController, pose: PoseGdxModel)

    class SequenceRenderer(context: BattleContext, figureModel: FigureGdxModel) : FigureRenderer() {
        var atlas: TextureAtlas? = context.atlases[figureModel.atlas]!!
        val allTextures = if (figureModel.render == GdxRenderType.SEQUENCE) filterAtlas(atlas!!, figureModel.body).toList() else null
        var animation: Animation<TextureRegion>? = null

        override fun render(figure: FigureController, batch: SpriteBatch, bodyScale: Float, delta: Float) {
            animation?.let { animation ->
                if (figure.pose != "death" || !animation.isAnimationFinished(figure.timePassed - figure.timePoseStarted)) {
                    batch.draw(animation.getKeyFrame(figure.timePassed - figure.timePoseStarted, true),
                            figure.x - figure.figureModel.anchor_x * bodyScale * figure.flipMultiplier,
                            figure.y,
                            figure.figureModel.source_width * bodyScale * figure.flipMultiplier,
                            figure.figureModel.source_height * bodyScale)
                } else if (figure.pose == "death") {
                    if (figure.flipped || figure.position > 0) {
                        figure.battle.removeController(figure)
                    } else {
                        batch.draw(animation.keyFrames.last(),
                                figure.x - figure.figureModel.anchor_x * bodyScale * figure.flipMultiplier,
                                figure.y,
                                figure.figureModel.source_width * bodyScale * figure.flipMultiplier,
                                figure.figureModel.source_height * bodyScale)
                    }
                }

                if (animation.isAnimationFinished(figure.timePassed - figure.timePoseStarted) && animation.playMode == Animation.PlayMode.NORMAL
                        && figure.pose != "death" && !figure.isDead) {
                    figure.switchPose("idle")
                }
            }
        }

        override fun switchPose(figure: FigureController, pose: PoseGdxModel) {
            pose?.sound?.let { figure.player(it) }
            val playMode = when (figure.pose) {
                "idle" -> Animation.PlayMode.LOOP
                else -> Animation.PlayMode.NORMAL
            }
            this.animation = Animation(FigureController.FRAME_DURATION, Array(allTextures!!.subList((pose.start) - 1, (pose.end)).toTypedArray()), playMode)
        }
    }

    class SpineRenderer(context: BattleContext, figureModel: FigureGdxModel) : FigureRenderer() {
        val polygonBatch = PolygonSpriteBatch()
        val skeletonRenderer = SkeletonRenderer().apply {
            setPremultipliedAlpha(true)

        }
        val spineAtlas = context.atlases[figureModel.atlas]!!
        val json = SkeletonJson(spineAtlas)
        val jsonData = json.readSkeletonData(Gdx.files.internal("textures/personages/${figureModel.name}/${figureModel.name}.json"))
        val skeleton = Skeleton(jsonData).apply {
            if (jsonData.skins.firstOrNull { it.name == "Dark_knight" } != null) {
                setSkin("Dark_knight")
            }
        }
        val stateData = AnimationStateData(jsonData)
        val spineAnimation = AnimationState(stateData).apply {
            setAnimation(0, getIdlePoseName(), true)
        }

        override fun render(figure: FigureController, batch: SpriteBatch, bodyScale: Float, delta: Float) {
            figure.apply {
                skeleton.scaleX = (if (flipped xor figureModel.invert_x) -1f else 1f) * figureModel.scale * battle.scale
                skeleton.scaleY = figureModel.scale * battle.scale
                skeleton.setPosition(x + figure.figureModel.shift_x * figure.figureModel.scale * battle.scale, y + figure.figureModel.shift_y * figure.figureModel.scale * battle.scale)

                spineAnimation.update(delta)
                spineAnimation.apply(skeleton)
                skeleton.updateWorldTransform()

                batch.end()
                polygonBatch.begin()
                skeletonRenderer.draw(polygonBatch, skeleton)
                polygonBatch.end()
                batch.begin()
            }
        }

        override fun switchPose(figure: FigureController, pose: PoseGdxModel) {
            //TODO: Remove this crunch
            pose?.sound?.let { figure.player(it) }
            val poseName = if (pose.name == "idle") getIdlePoseName() else pose.name
            spineAnimation.setAnimation(0, poseName, pose.name == "idle")
            if (pose.name != "Death") {
                spineAnimation.addAnimation(0, getIdlePoseName(), true, 0f)
            }
            spineAnimation.addListener(object : DefaultAnimationStateListener() {
                override fun complete(entry: AnimationState.TrackEntry?) {
                    if (entry?.animation?.name == "Death") {
                        if (figure.flipped || figure.position > 0) {
                            figure.battle.removeController(figure)
                        }
                    }
                    if (entry?.animation?.name == pose.name) {
                        figure.figureModel.poses?.firstOrNull { it.name == pose.name }?.let { pose ->
                            val shiftMulti = if (figure.flipped xor figure.figureModel.invert_x) -1f else 1f
                            val dx = pose.isx * figure.battle.scale * figure.figureModel.scale * shiftMulti
                            val dy = pose.isy * figure.battle.scale * figure.figureModel.scale
                            figure.x += dx
                            figure.originX += dx
                            figure.fromX += dx
                            figure.targetX += dx
                            figure.y += dy
                            figure.originY += dy
                            figure.fromY += dy
                            figure.targetY += dy
                        }
                    }
                }
            })
        }

        private fun getIdlePoseName() = jsonData.animations.firstOrNull { it.name == "idle" }?.name
                ?: "Idle"
    }
}

data class FigurePostponedEffectWrapper(
        val model: PoseEffectGdxModel,
        var timeLeft: Float,
        val descriptor: EffectDescriptor?
)

/**
 * GDX graphics figure controller with poses
 */
class FigureController(
        context: BattleContext,
        battle: BattleController,
        id: Int,
        y: Float,
        val figureModel: FigureGdxModel,
        var viewModel: UnitViewModel,
        internal val player: (String) -> Unit
) : ElementController(context, battle, id) {

    var originX: Float = 0f

    var originY: Float = y

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

    val frozenTexture = context.battleAtlas.findRegion("ailment_frozen")

    var isDead = false

    lateinit var pose: String
    val flipped = viewModel.team > 0
    internal val flipMultiplier = if (flipped) -1 else 1

    var position: Int = 0

    val renderer: FigureRenderer

    val effectsToShow = mutableListOf<FigurePostponedEffectWrapper>()

    init {
        renderer = when (figureModel.render) {
            GdxRenderType.SEQUENCE -> FigureRenderer.SequenceRenderer(context, figureModel)
            GdxRenderType.SPINE -> FigureRenderer.SpineRenderer(context, figureModel)
        }
        switchPose("idle")
    }

    override fun render(batch: SpriteBatch, delta: Float) {
        timePassed += delta

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
        renderer.render(this@FigureController, batch, bodyScale, delta)
        if (viewModel.stats.isFrozen) {
            batch.draw(frozenTexture, x - 64f * bodyScale * flipMultiplier, y, 128f * bodyScale * flipMultiplier, 64f * bodyScale)
        }

        effectsToShow.forEach { effect ->
            effect.timeLeft -= delta
            if (effect.timeLeft <= 0f) {
                battle.showEffect(effect.descriptor
                        ?: EffectDescriptor.AttachedToFigure(this@FigureController, true), effect.model)
            }
        }
        effectsToShow.removeAll { it.timeLeft <= 0f }
    }

    fun switchPose(poseName: String, descriptor: EffectDescriptor? = null) {
        if (isDead) return
        timePoseStarted = timePassed
        this.pose = poseName
        figureModel.poses?.firstOrNull { it.name == pose }?.let { pose ->
            renderer.switchPose(this@FigureController, pose)
            pose.effect?.let { effect ->
                effectsToShow.add(FigurePostponedEffectWrapper(effect, effect.frame * FRAME_DURATION, descriptor))
            }
        }
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

        figureModel.poses?.firstOrNull { it.name == pose }?.let { pose ->
            originX = targetX - pose.isx * battle.scale * figureModel.scale
            originY = targetY - pose.isy * battle.scale * figureModel.scale
        }

        if (needFastMove) {
            move(originX, targetY, duration)
        } else {
            if (pose == "idle") {
                x = if (flipped) Gdx.graphics.width + figureModel.width * figureModel.scale * battle.scale * 2f else -2f * figureModel.width * figureModel.scale * battle.scale
                move(originX, originY, 2f / (position + 1))
            } else {
                x = originX
                fromX = originX
                this.targetX = originX
                fromY = originY
                this.targetY = originY
                y = originY
            }
        }
    }

    companion object {
        const val FRAME_DURATION = 1 / 60f
    }
}