package com.game7th.swipe.game.actors

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.game7th.battle.dto.TileViewModel
import com.game7th.swipe.GdxGameContext

class TileView(
        private val gameContext: GdxGameContext,
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

    private var skillImage: Image? = null
    private var stackSizeLabel: Label = Label("", Label.LabelStyle(gameContext.font, Color.WHITE)).apply {
        setFontScale(size / 4 / 36f)
        x = size - width - size/4f
        y = size/4f
        setAlignment(Align.right)
        zIndex = 3
    }

    private var stunImage: Image? = null

    private var drawnSectors = 0
    private var angle = 10f
    private var sectorRotation = 0f

    var tx: Int = 0
    var ty: Int = 0
    var uvm: TileViewModel? = null
    var removed = false

    private val bufferVector = Vector2()

    init {
        width = size
        height = size
        updateFrom(viewModel)
        addActor(stackSizeLabel)
    }

    override fun act(delta: Float) {
        super.act(delta)
        sectorRotation += (45f + 90f * (vm?.stackSize?:0) / (vm?.maxStackSize?:0)) * delta
        sectorRotation %= 360f
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        super.draw(batch, parentAlpha)
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

            shapeRenderer
            shapeRenderer.end()
            batch.begin()
        }
    }

    fun updateFrom(viewModel: TileViewModel) {
        val isSkinChanged = vm?.skin != viewModel.skin
        val isStackSizeChanged = vm?.stackSize != viewModel.stackSize
        val isStunChanged = vm?.stun != viewModel.stun

        vm = viewModel
        name = "${viewModel.id}"

        if (isSkinChanged) {
            removeSkill()
            addSkill()
        }

        if (isStackSizeChanged) {
            stackSizeLabel.setText(if (viewModel.stackSize < 1) "" else "${viewModel.stackSize}")
            stackSizeLabel.isVisible = drawnSectors == 0
            if (viewModel.stackSize >= viewModel.maxStackSize && viewModel.maxStackSize > 1) {
                skillImage?.color = Color.BLUE
            } else {
                skillImage?.color = Color.WHITE
            }
        }

        if (isStunChanged) {
            if (vm?.stun == true) {
                //appear stun
                stunImage = Image(gameContext.battleAtlas.findRegion("tile_stun")).apply {
                    width = size
                    height = size
                    originX = 18f
                    originY = 18f
                    zIndex = 3
                }
                addActor(stunImage)
            } else {
                stunImage?.remove()
                stunImage = null
            }
        }
    }

    private fun addSkill() {
        vm?.let { vm ->
            skillImage = Image(gameContext.battleAtlas.findRegion(vm.skin)).apply {
                zIndex = 2
                originY = 18f
                originX = 18f
                width = size
                height = size
            }

            addActor(skillImage)
        }
    }

    private fun removeSkill() {
        skillImage?.let {
            removeActor(it)
        }.also { skillImage = null }
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
