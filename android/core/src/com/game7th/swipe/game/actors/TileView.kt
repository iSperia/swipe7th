package com.game7th.swipe.game.actors

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.game7th.battle.event.TileViewModel
import com.game7th.swipe.GdxGameContext

interface TileDoubleTapCallback {
    fun processDoubleTapped(id: Int)
}

class TileView(
        private val gameContext: GdxGameContext,
        viewModel: TileViewModel,
        doubleTapCallback: TileDoubleTapCallback
) : Group() {

    private var vm: TileViewModel? = null

    private var skillImage: Image? = null
    private var stackSizeLabel: Label? = null

    init {
        originX = 18f
        originY = 18f
        width = 36f
        height = 36f

        updateFrom(viewModel)

        addListener(object : ClickListener() {

            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (tapCount >= 2) {
                    println("Double clicked")
                    doubleTapCallback.processDoubleTapped(vm?.id ?: -1)
                    tapCount = 0
                }
            }
        })
    }

    fun updateFrom(viewModel: TileViewModel) {
        val isSkinChanged = vm?.skin != viewModel.skin
        val isStackSizeChanged = vm?.stackSize != viewModel.stackSize

        vm = viewModel
        name = "${viewModel.id}"

        if (isSkinChanged) {
            removeSkill()
            addSkill()
        }

        if (isStackSizeChanged) {
            removeStackSizeLabel()
            addStackSizeLabel()
            if (viewModel.stackSize >= viewModel.maxStackSize && viewModel.maxStackSize > 1) {
                skillImage?.color = Color.BLUE
            } else {
                skillImage?.color = Color.WHITE
            }
        }
    }

    private fun addStackSizeLabel() {
        vm?.let { vm ->
            val value = if (vm.stackSize < 2) "" else "${vm.stackSize}"
            stackSizeLabel = Label(value, Label.LabelStyle(gameContext.font, Color.WHITE)).apply {
                setFontScale(0.5f)
                originX = width
                originY = 0f
                x = 36f - width
                y = 0f
                zIndex = 3
            }

            addActor(stackSizeLabel)
        }
    }

    private fun addSkill() {
        vm?.let { vm ->
            skillImage = Image(gameContext.atlas.findRegion(vm.skin)).apply {
                zIndex = 2
                originY = 18f
                originX = 18f
                width = 36f
                height = 36f
            }

            addActor(skillImage)
        }
    }

    private fun removeStackSizeLabel() {
        stackSizeLabel?.let {
            removeActor(it)
        }.also { stackSizeLabel = null }
    }

    private fun removeSkill() {
        skillImage?.let {
            removeActor(it)
        }.also { skillImage = null }
    }

    fun applyPosition(x: Int, y: Int) {
        this.x = 36f * x + 18f
        this.y = 36f * (4 - y)
    }
}