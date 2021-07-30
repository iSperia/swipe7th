package com.game7th.swipe.game.actors

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.Group
import com.esotericsoftware.spine.*
import com.game7th.swipe.game.BattleContext
import com.game7th.swiped.api.battle.TileViewModel

class TileView(
        private val context: BattleContext,
        viewModel: TileViewModel,
        private val size: Float,
        private val shapeRenderer: ShapeRenderer
) : Group() {

    private var vm: TileViewModel? = null
        set(value) {
            field = value
            drawnSectors = value?.maxStackSize ?: 0
            angle = if (drawnSectors > 0) 360f / drawnSectors else 0f
        }

    private var drawnSectors = 0
    private var angle = 10f
    private var sectorRotation = 0f

    val polygonBatch = PolygonSpriteBatch()
    val skeleton: Skeleton
    var stateData: AnimationStateData
    var animation: AnimationState
    val skeletonRenderer = SkeletonRenderer().apply {
        premultipliedAlpha = true
    }

    var tx: Int = 0
    var ty: Int = 0
    var nextAction: Float = 0f
    var removed = false

    val durationActionQueue = mutableListOf<Action>()
    private val bufferVector = Vector2()

    init {
        width = size
        height = size
        val skeletonJson = context.skeletons[viewModel.skin]!!
        skeleton = Skeleton(skeletonJson)
        stateData = AnimationStateData(skeletonJson)
        animation = AnimationState(stateData).apply {
            setAnimation(0, "idle", true)
        }
        updateFrom(viewModel)
    }

    override fun act(delta: Float) {
        super.act(delta)
        sectorRotation += (45f + 90f * (vm?.stackSize ?: 0) / (vm?.maxStackSize ?: 0)) * delta
        sectorRotation %= 360f

        if (!hasActions() && durationActionQueue.isNotEmpty()) {
            addAction(durationActionQueue.removeAt(0))
        }
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        bufferVector.set(0f, 0f)
        localToStageCoordinates(bufferVector)

        animation.update(Gdx.graphics.deltaTime)
        val scale = 0.92f * size / 180f
        skeleton.scaleX = scale
        skeleton.scaleY = scale
        skeleton.setPosition(bufferVector.x + 0.04f * size, bufferVector.y + 0.04f * size)
        animation.apply(skeleton)
        skeleton.updateWorldTransform()

        batch.end()
        polygonBatch.begin()
        skeletonRenderer.draw(polygonBatch, skeleton)
        polygonBatch.end()
        batch.begin()

        if (drawnSectors > 1) {
            batch.end()

            shapeRenderer.projectionMatrix = batch.projectionMatrix
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

            bufferVector.set(0f, 0f)
            localToStageCoordinates(bufferVector)

            (0..drawnSectors).forEach {
                val color = if (vm?.stackSize ?: 0 >= it) Color.WHITE else Color.DARK_GRAY
                shapeRenderer.strokeArc(
                        strokeWidth = 0.06f * size * scaleX,
                        x = bufferVector.x + size / 2,
                        y = bufferVector.y + size / 2,
                        radius = size * 0.4f * scaleX,
                        start = sectorRotation + angle * it + 3f,
                        degrees = angle - 6f,
                        sampling = 2f,
                        color = color)
            }

            shapeRenderer.end()
            batch.begin()
        }
    }

    fun updateFrom(viewModel: TileViewModel) {
        vm = viewModel
    }
}

/** Draws an arc with 'stroke' of given width  */
fun ShapeRenderer.strokeArc(strokeWidth: Float, x: Float, y: Float, radius: Float, start: Float, degrees: Float, sampling: Float = 2f, color: Color = Color.WHITE) {
    val segments = ((6 * Math.cbrt(radius.toDouble()) * (Math.abs(degrees) / 360.0f)) * sampling).toInt()
    val colorBits = color.toFloatBits()

    for (i in 0 until segments) {
        val x1 = radius * MathUtils.cosDeg(start + (degrees / segments) * i)
        val y1 = radius * MathUtils.sinDeg(start + (degrees / segments) * i)

        val x2 = (radius - strokeWidth) * MathUtils.cosDeg(start + (degrees / segments) * i)
        val y2 = (radius - strokeWidth) * MathUtils.sinDeg(start + (degrees / segments) * i)

        val x3 = radius * MathUtils.cosDeg(start + (degrees / segments) * (i + 1))
        val y3 = radius * MathUtils.sinDeg(start + (degrees / segments) * (i + 1))

        val x4 = (radius - strokeWidth) * MathUtils.cosDeg(start + (degrees / segments) * (i + 1))
        val y4 = (radius - strokeWidth) * MathUtils.sinDeg(start + (degrees / segments) * (i + 1))

        renderer.color(colorBits)
        renderer.vertex(x + x1, y + y1, 0f)
        renderer.color(colorBits)
        renderer.vertex(x + x3, y + y3, 0f)
        renderer.color(colorBits)
        renderer.vertex(x + x2, y + y2, 0f)

        renderer.color(colorBits)
        renderer.vertex(x + x3, y + y3, 0f)
        renderer.color(colorBits)
        renderer.vertex(x + x2, y + y2, 0f)
        renderer.color(colorBits)
        renderer.vertex(x + x4, y + y4, 0f)
    }
}
