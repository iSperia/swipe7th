package com.game7th.swipe.game.actors

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.game7th.battle.event.TileViewModel
import com.game7th.swipe.GdxGameContext

class TileView(
        private val gameContext: GdxGameContext,
        viewModel: TileViewModel,
        private val size: Float
) : Group() {

    private var vm: TileViewModel? = null

    private var skillImage: Image? = null
    private var stackSizeLabel: Label? = null

    var tx: Int = 0
    var ty: Int = 0
    var uvm: TileViewModel? = null
    var removed = false

    init {
        width = size
        height = size
        updateFrom(viewModel)
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
                setFontScale(size / 4 / 36f)
                x = size - width - size/8f
                y = size/8f
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
                width = size
                height = size
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
}