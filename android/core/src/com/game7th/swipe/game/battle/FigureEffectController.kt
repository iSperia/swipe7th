package com.game7th.swipe.game.battle

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.utils.AnimationController
import com.esotericsoftware.spine.*
import com.game7th.swipe.game.BattleContext
import com.game7th.swipe.game.battle.model.PoseEffectGdxModel

class FigureEffectController(
        context: BattleContext,
        battle: BattleController,
        id: Int,
        private val targetFigure: FigureController,
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

    override fun render(batch: SpriteBatch, delta: Float) {
        skeleton.scaleX = (if (targetFigure.flipped) -1f else 1f) * targetFigure.figureModel.scale * battle.scale
        skeleton.scaleY = targetFigure.figureModel.scale * battle.scale
        skeleton.setPosition(targetFigure.x + effect.x * battle.scale, targetFigure.y + effect.y * battle.scale)

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