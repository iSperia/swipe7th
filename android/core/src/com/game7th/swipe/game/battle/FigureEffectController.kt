package com.game7th.swipe.game.battle

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.utils.AnimationController
import com.esotericsoftware.spine.*
import com.game7th.swipe.game.BattleContext
import com.game7th.swipe.game.battle.model.PoseEffectGdxModel
import kotlin.math.min

sealed class EffectDescriptor {
    data class AttachedToFigure(
            val targetFigure: FigureController,
            val direction: Boolean
    ) : EffectDescriptor()

    data class SlideToFigure(
            val from: FigureController,
            val to: FigureController,
            val duration: Float
    ) : EffectDescriptor()
}

class FigureEffectController(
        context: BattleContext,
        battle: BattleController,
        id: Int,
        private val descriptor: EffectDescriptor,
        private val effect: PoseEffectGdxModel
) : ElementController(context, battle, id) {

    val polygonBatch = PolygonSpriteBatch()
    val skeletonRenderer = SkeletonRenderer().apply {
        setPremultipliedAlpha(true)

    }
    val spineAtlas = context.atlases[effect.id]!!
    val json = SkeletonJson(spineAtlas)
    val jsonData = json.readSkeletonData(Gdx.files.internal("textures/effects/${effect.id}/${effect.id}.json"))
    val skeleton = Skeleton(jsonData).apply {
        if (jsonData.skins.firstOrNull { it.name == "Dark_knight" } != null) {
            setSkin("Dark_knight")
        }
    }
    val stateData = AnimationStateData(jsonData)
    val spineAnimation = AnimationState(stateData).apply {
        setAnimation(0, effect.pose, false)
        addListener(object : AnimationState.AnimationStateListener {
            override fun start(entry: AnimationState.TrackEntry?) {}
            override fun interrupt(entry: AnimationState.TrackEntry?) {}
            override fun dispose(entry: AnimationState.TrackEntry?) {}
            override fun complete(entry: AnimationState.TrackEntry?) {}
            override fun event(entry: AnimationState.TrackEntry?, event: Event?) {}
            override fun end(entry: AnimationState.TrackEntry?) {
                battle.removeController(this@FigureEffectController)
            }
        })
    }

    var timePassed: Float = 0f

    override fun render(batch: SpriteBatch, delta: Float) {
        timePassed += delta
        when (descriptor) {
            is EffectDescriptor.AttachedToFigure -> {
                skeleton.scaleX = (if (descriptor.targetFigure.flipped xor descriptor.targetFigure.figureModel.invert_x) -1f else 1f) * (if (descriptor.direction) descriptor.targetFigure.figureModel.scale else 1f) * battle.scale
                skeleton.scaleY = descriptor.targetFigure.figureModel.scale * battle.scale
                skeleton.setPosition(descriptor.targetFigure.x + effect.x * battle.scale, descriptor.targetFigure.y + effect.y * battle.scale)

            }
            is EffectDescriptor.SlideToFigure -> {
                skeleton.scaleX = (if (descriptor.from.flipped xor descriptor.from.figureModel.invert_x) -1f else 1f) * descriptor.from.figureModel.scale * battle.scale
                skeleton.scaleY = descriptor.from.figureModel.scale * battle.scale
                val progress = min(1f, timePassed / descriptor.duration)
                skeleton.setPosition(descriptor.from.x + (descriptor.to.x - descriptor.from.x) * progress,
                    descriptor.from.y + (descriptor.to.y - descriptor.from.y) * progress)
            }
        }

        spineAnimation.update(delta)
        spineAnimation.apply(skeleton)
        skeleton.updateWorldTransform()

        batch.end()
        polygonBatch.begin()
        skeletonRenderer.draw(polygonBatch, skeleton)
        polygonBatch.end()
        batch.begin()
    }

    override fun dispose() {
        super.dispose()
    }
}