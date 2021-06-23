@file:Suppress("NAME_SHADOWING")

package com.game7th.swipe.game.battle

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.*
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.utils.Array
import com.esotericsoftware.spine.*
import com.game7th.swipe.game.GameContextWrapper
import com.game7th.swipe.game.battle.model.FigureGdxModel
import com.game7th.swipe.game.battle.model.GdxRenderType
import com.game7th.swiped.api.battle.PersonageViewModel

/**
 * GDX graphics figure controller with poses
 */
class FigureController(
        context: GameContextWrapper,
        battle: BattleController,
        id: Int,
        y: Float,
        val appearStrategy: Int,
        val figureModel: FigureGdxModel,
        var viewModel: PersonageViewModel,
        val player: (String) -> Unit
) : ElementController(context, battle, id) {

    var originX: Float = 0f

    var originY: Float = y

    var x = originX
    var y = originY

    var timePassed = 0f
    var timePoseStarted = 0f
    var timeMoveStarted = 0f
    var timeMoveFinished = 0f
    var timeShift = 0f

    var fromX = x
    var fromY = y
    var targetX = x
    var targetY = y

    val frozenTexture = context.gameContext.battleAtlas.findRegion("ailment_frozen")

    var atlas: TextureAtlas? = if (figureModel.render == GdxRenderType.SEQUENCE) context.atlases[figureModel.atlas]!! else null
    val allTextures = if (figureModel.render == GdxRenderType.SEQUENCE) filterAtlas(atlas!!, figureModel.body).toList() else null
    var animation: Animation<TextureRegion>? = null

    var isDead = false

    val polygonBatch = PolygonSpriteBatch()
    val renderer = SkeletonRenderer().apply {
        setPremultipliedAlpha(true)

    }

    lateinit var pose: String
    val flipped = viewModel.team > 0
    private val flipMultiplier = if (flipped) -1 else 1

    var position: Int = 0

    val dragonAtlas = TextureAtlas(Gdx.files.internal("prince.atlas"))
    val json = SkeletonJson(dragonAtlas)
    val jsonData = json.readSkeletonData(Gdx.files.internal("prince.json"))
    val skeleton = Skeleton(jsonData)
    val stateData = AnimationStateData(jsonData)
    val dragonAnimation = AnimationState(stateData).apply {
        setAnimation(0, "idle", true)
    }

    init {
        switchPose("idle")
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
        when (figureModel.render) {
            GdxRenderType.SEQUENCE -> {
                animation?.let { animation ->
                    if (pose != "death" || !animation.isAnimationFinished(timePassed - timePoseStarted)) {
                        batch.draw(animation.getKeyFrame(timePassed - timePoseStarted, true),
                                x - figureModel.anchor_x * bodyScale * flipMultiplier,
                                y,
                                figureModel.source_width * bodyScale * flipMultiplier,
                                figureModel.source_height * bodyScale)
                    } else if (pose == "death") {
                        if (flipped || position > 0) {
                            battle.removeController(this)
                        } else {
                            batch.draw(animation.keyFrames.last(),
                                    x - figureModel.anchor_x * bodyScale * flipMultiplier,
                                    y,
                                    figureModel.source_width * bodyScale * flipMultiplier,
                                    figureModel.source_height * bodyScale)
                        }
                    }

                    if (animation.isAnimationFinished(timePassed - timePoseStarted) && animation.playMode == Animation.PlayMode.NORMAL
                            && pose != "death" && !isDead) {
                        switchPose("idle")
                    }
                }
            }
            GdxRenderType.SPINE -> {
                skeleton.scaleX = (if (flipped) -1f else 1f) * figureModel.scale * battle.scale
                skeleton.scaleY = figureModel.scale * battle.scale
                skeleton.setPosition(x, y)

                dragonAnimation.update(delta)
                dragonAnimation.apply(skeleton)
                skeleton.updateWorldTransform()

                batch.end()
                polygonBatch.begin()
                renderer.draw(polygonBatch, skeleton)
                polygonBatch.end()
                batch.begin()
            }
        }

        if (viewModel.stats.isFrozen) {
            batch.draw(frozenTexture, x - 64f * bodyScale * flipMultiplier, y, 128f * bodyScale * flipMultiplier, 64f * bodyScale)
        }

    }

    fun switchPose(poseName: String) {
        if (isDead) return
        timePoseStarted = timePassed
        this.pose = poseName
        val pose = figureModel.poses?.firstOrNull { it.name == pose }
        val playMode = when (this.pose) {
            "idle" -> Animation.PlayMode.LOOP
            "attack" -> Animation.PlayMode.NORMAL
            "damage" -> Animation.PlayMode.NORMAL
            "death" -> Animation.PlayMode.NORMAL
            "ability" -> Animation.PlayMode.NORMAL
            else -> Animation.PlayMode.NORMAL
        }
        when (figureModel.render) {
            GdxRenderType.SEQUENCE -> {
                pose?.sound?.let { player(it) }
                this.animation = Animation(FRAME_DURATION, Array(allTextures!!.subList((pose?.start ?: 1) - 1, (pose?.end ?: 1)).toTypedArray()), playMode)
            }
            GdxRenderType.SPINE -> {
                dragonAnimation.setAnimation(0, poseName, playMode == Animation.PlayMode.LOOP)
                if (poseName != "Death") {
                    dragonAnimation.addAnimation(0, "idle", true, 0f)
                }
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

        originX = targetX
        originY = targetY

        if (needFastMove) {
            move(originX, targetY, duration)
        } else {
            if (appearStrategy == 0) {
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
        const val FRAME_DURATION = 1/30f
    }
}