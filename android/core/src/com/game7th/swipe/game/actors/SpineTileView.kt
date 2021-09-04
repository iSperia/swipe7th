package com.game7th.swipe.game.actors

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.utils.FloatArray
import com.esotericsoftware.spine.*
import com.game7th.swipe.game.BattleContext
import com.game7th.swiped.api.battle.TileViewModel

class SpineTileView(
        private val context: BattleContext,
        viewModel: TileViewModel,
        private val size: Float,
        private val polygonBatch: PolygonSpriteBatch,
        private val tileBatch: PolygonSpriteBatch,
        private val progressIndicatorEmpty: TextureRegion?,
        private val progressIndicatorFull: TextureRegion?,
        private val tileBackground: TextureRegion?
) : AbstractTileView(), Comparable<Actor>, LayerProvider {

    private var vm: TileViewModel? = null
        set(value) {
            field = value
            drawnSectors = value?.maxStackSize ?: 0
            angle = if (drawnSectors > 0) 360f / drawnSectors else 0f
        }

    private var drawnSectors = 0
    private var angle = 10f
    private var sectorRotation = 0f

    val skeleton: Skeleton
    var stateData: AnimationStateData
    var animation: AnimationState
    val skeletonRenderer = SkeletonRenderer().apply {
        premultipliedAlpha = true
    }

    var nextAction: Float = 0f



    private val bufferVector = Vector2()

    init {
        width = size
        height = size
        println("Skeleton skin: ${viewModel.skin}")
        val skeletonJson = context.skeletons[viewModel.skin]!!
        skeleton = Skeleton(skeletonJson)
        stateData = AnimationStateData(skeletonJson)
        animation = AnimationState(stateData).apply {
            setAnimation(0, "Attract", false)
            addAnimation(0, "Idle", true, 0f)
        }
        updateFrom(viewModel)

        tileBackground?.let {
            val image = Image(it).apply {
                width = size
                height = size
            }
            addActor(image)
        }
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        bufferVector.set(0f, 0f)
        localToStageCoordinates(bufferVector)

        animation.update(Gdx.graphics.deltaTime)
        val scale = size * scaleX / 180f
        skeleton.scaleX = scale
        skeleton.scaleY = scale
        skeleton.setPosition(bufferVector.x + size / 2f, bufferVector.y + size / 2f)
        animation.apply(skeleton)
        skeleton.updateWorldTransform()

        if (drawnSectors > 1 && progressIndicatorEmpty != null && progressIndicatorFull != null) {
            batch.end()
            tileBatch.begin()

            (0 until drawnSectors).filter { sectorRotation + angle * (it+1) <= 180f }.forEach {
                tileBatch.strokeArc(
                        strokeWidth = 0.11f * size,
                        x = bufferVector.x + size / 2,
                        y = bufferVector.y + size / 2,
                        radius = size * 0.4f,
                        start = sectorRotation + angle * it + 3f,
                        degrees = angle - 6f,
                        sampling = 2f,
                        if ((vm?.stackSize ?: 0) - 1 >= it) progressIndicatorFull else progressIndicatorEmpty)
            }

            tileBatch.end()
            batch.begin()
        }

        batch.end()
        polygonBatch.begin()
        skeletonRenderer.draw(polygonBatch, skeleton)
        polygonBatch.end()
        batch.begin()

        if (drawnSectors > 1 && progressIndicatorEmpty != null && progressIndicatorFull != null) {
            batch.end()
            tileBatch.begin()

            (0 until drawnSectors).filter { sectorRotation + angle * (it+1) > 180f }.forEach {
                tileBatch.strokeArc(
                        strokeWidth = 0.11f * size,
                        x = bufferVector.x + size / 2,
                        y = bufferVector.y + size / 2,
                        radius = size * 0.4f,
                        start = sectorRotation + angle * it + 3f,
                        degrees = angle - 6f,
                        sampling = 2f,
                        if ((vm?.stackSize ?: 0) - 1 >= it) progressIndicatorFull else progressIndicatorEmpty)
            }

            tileBatch.end()
            batch.begin()
        }
    }

    override fun updateFrom(viewModel: TileViewModel) {
        vm = viewModel
    }

    override fun compareTo(other: Actor): Int {
        return if (other is LayerProvider) getLayer().compareTo(other.getLayer()) else 0
    }

    override fun getLayer(): Int {
        return vm?.layer ?: 100
    }
}

/** Draws an arc with 'stroke' of given width  */
fun PolygonSpriteBatch.strokeArc(strokeWidth: Float, x: Float, y: Float, radius: Float, start: Float, degrees: Float, sampling: Float = 2f, texture: TextureRegion) {
    val segments = (degrees / 12).toInt()

    val color = Color.WHITE.toFloatBits()
    val verticeCount = (segments + 1) * 2
    val vertices = FloatArray(verticeCount * 5)
    val degreeDelta = degrees / segments

    for (i in 0..segments) {
        /**Close to center vertex*/
        /*x*/vertices.add(x + (radius - strokeWidth) * MathUtils.cosDeg(start + degreeDelta * i))
        /*y*/vertices.add(y + (radius - strokeWidth) * MathUtils.sinDeg(start + degreeDelta * i))
        /*c*/vertices.add(color)
        /*u*/vertices.add(texture.u + (texture.u2 - texture.u) * i.toFloat() / segments)
        /*v*/vertices.add(texture.v)

        /**Remote radius vertex*/
        /*x*/vertices.add(x + radius * MathUtils.cosDeg(start + degreeDelta * i))
        /*y*/vertices.add(y + radius * MathUtils.sinDeg(start + degreeDelta * i))
        /*c*/vertices.add(color)
        /*u*/vertices.add(texture.u + (texture.u2 - texture.u) * i.toFloat() / segments)
        /*v*/vertices.add(texture.v2)
    }

    val triangles = ShortArray(segments * 2 * 3) { index -> (index / 3 + index % 3).toShort() }
    draw(texture.texture, vertices.toArray(), 0, verticeCount * 5, triangles, 0, triangles.size)
}
