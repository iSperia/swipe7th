package com.game7th.swipe.game.actors

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.actions.*
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.game7th.battle.event.TileViewModel
import com.game7th.swipe.GdxGameContext
import ktx.actors.alpha
import ktx.actors.repeatForever

interface TileDoubleTapCallback {
    fun processDoubleTapped(id: Int)
}

class TileView(
        private val gameContext: GdxGameContext,
        viewModel: TileViewModel,
        doubleTapCallback: TileDoubleTapCallback
) : Group() {

    private var vm: TileViewModel? = null

    private var backgroundImage: Image? = null
    private var skillImage: Image? = null
    private var foregroundImage: Group? = null
    private var stackSizeLabel: Label? = null

    init {
        originX = 16f
        originY = 16f
        width = 32f
        height = 32f

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
        val isBackgroundChanged = vm?.background != viewModel.background || vm?.backgroundIndex != viewModel.backgroundIndex
        val isForegroundChanged = vm?.fractionForeground != viewModel.fractionForeground
        val isSkinChanged = isBackgroundChanged || vm?.skin != viewModel.skin
        val isStackSizeChanged = vm?.stackSize != viewModel.stackSize

        vm = viewModel
        name = "${viewModel.id}"

        if (isBackgroundChanged) {
            removeBackground()
            if (vm?.background != null) {
                addBackground()
            }
        }

        if (isForegroundChanged) {
            removeForeground()
            addForeground()
        }

        if (isSkinChanged) {
            removeSkill()
            addSkill()
        }

        if (isStackSizeChanged) {
            removeStackSizeLabel()
            addStackSizeLabel()
        }
    }

    private fun addStackSizeLabel() {
        vm?.let { vm ->
            val value = if (vm.stackSize < 2) "" else "${vm.stackSize}"
            stackSizeLabel = Label(value, Label.LabelStyle(gameContext.font, Color.WHITE)).apply {
                setFontScale(0.5f)
                originX = width
                originY = 0f
                x = 32f - width
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
            }

            addActor(skillImage)
        }
    }

    private fun addBackground() {
        vm?.let { vm ->
            backgroundImage = Image(gameContext.atlas.findRegion(vm.background, vm.backgroundIndex ?: 0)).apply {
                zIndex = 1
                originX = 16f
                originY = 16f
            }

            addActor(backgroundImage)
        }
    }

    private fun addForeground() {
        vm?.let { vm ->
            foregroundImage = Group().apply { zIndex = 4 }
            for (i in 0..5) {
                foregroundImage?.addActor(Image(gameContext.atlas.findRegion("tile_fg_${vm.fractionForeground}")).apply {
                    originX = 16f
                    originY = 16f
                    rotation = i * 360f / 6
                    setScale(1.1f)
                    addAction(ParallelAction(
                            RotateByAction().apply {
                                amount = 360f
                                duration = 2f
                            }.repeatForever(),
                            SequenceAction(
                                    ScaleToAction().apply {
                                        setScale(0.9f, 0.9f)
                                        duration = 0.3f
                                    },
                                    ScaleToAction().apply {
                                        setScale(1.1f, 1.1f)
                                        duration = 0.3f
                                    }
                            ).repeatForever()
                    ))
                    alpha = 0f
                    addAction(AlphaAction().apply {
                        alpha = 1f
                        duration = 0.5f
                    })
                })
            }
            addActor(foregroundImage)
        }
    }

    private fun removeStackSizeLabel() {
        stackSizeLabel?.let {
            removeActor(it)
        }.also { stackSizeLabel = null }
    }

    private fun removeForeground() {
        foregroundImage?.let {
            it.children.forEach { it.clearActions() }
            removeActor(it)
        }.also { foregroundImage = null }
    }

    private fun removeSkill() {
        skillImage?.let {
            removeActor(it)
        }.also { skillImage = null }
    }

    private fun removeBackground() {
        backgroundImage?.let {
            removeActor(it)
        }.also { backgroundImage = null }
    }

    fun applyPosition(x: Int, y: Int) {
        this.x = 32f * x
        this.y = 32f * (5 - y)
    }
}