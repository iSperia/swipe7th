package com.game7th.swipe.game.actors

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.actions.AlphaAction
import com.badlogic.gdx.scenes.scene2d.actions.RunnableAction
import com.badlogic.gdx.scenes.scene2d.actions.ScaleToAction
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.game7th.battle.personage.PersonageViewModel
import com.game7th.swipe.GdxGameContext

class PersonageActor(
        private val context: GdxGameContext,
        private var vm: PersonageViewModel
) : Group() {

    var body: Image? = null
    val healthBarGreen: Image
    val healthBarRed: Image
    val healthAmount: Label

    init {
        showBody(vm)

        healthBarRed = Image(context.atlas.findRegion("health_bar_red")).apply {
            y = 50f
            x = 10f
            width = 40f
            height = 2f
        }
        addActor(healthBarRed)

        healthBarGreen = Image(context.atlas.findRegion("health_bar_green")).apply {
            y = 50f
            x = 10f
            width = 40f
            scaleX = vm.stats.health.toFloat() / vm.stats.maxHealth
            height = 2f
        }
        addActor(healthBarGreen)

        healthAmount = Label("${vm.stats.health}", Label.LabelStyle(context.font, Color.BLUE)).apply {
            x = 10f
            y = 20f
            zIndex = 10
        }
        addActor(healthAmount)
    }

    private fun hideBody() {
        val oldBody = body
        oldBody?.zIndex = 0
        oldBody?.addAction(SequenceAction(
                AlphaAction().apply {
                    alpha = 0f
                    duration = 0.2f
                },
                RunnableAction().apply { setRunnable {
                    oldBody.clearActions()
                    oldBody.remove()}
                }
        ))
    }

    private fun showBody(viewModel: PersonageViewModel) {
        body = Image(context.atlas.findRegion(if (viewModel.stats.health > 0) viewModel.skin else "personage_dead")).apply {
            y = 60f
            zIndex = 1
        }
        addActor(body)
    }

    fun updateFrom(viewModel: PersonageViewModel) {
        healthBarGreen.addAction(ScaleToAction().apply {
            setScale(viewModel.stats.health.toFloat() / viewModel.stats.maxHealth, 1f)
            duration = 0.2f
            healthAmount.setText("${viewModel.stats.health}")
        })
        if (vm.stats.health > 0 && viewModel.stats.health == 0) {
            hideBody()
            showBody(viewModel)
        }
        vm = viewModel
    }

}