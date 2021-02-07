package com.game7th.swipe.game.actors

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.actions.ScaleToAction
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.game7th.battle.personage.PersonageViewModel
import com.game7th.swipe.game.GdxGameContext

class PersonageActor(
        context: GdxGameContext,
        viewModel: PersonageViewModel
) : Group() {

    val body: Image
    val healthBarGreen: Image
    val healthBarRed: Image
    val healthAmount: Label

    init {
        body = Image(context.atlas.findRegion(viewModel.skin)).apply {
            y = 60f
        }
        addActor(body)

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
            scaleX = viewModel.stats.health.toFloat() / viewModel.stats.maxHealth
            height = 2f
        }
        addActor(healthBarGreen)

        healthAmount = Label("${viewModel.stats.health}/${viewModel.stats.maxHealth}", Label.LabelStyle(context.font, Color.BLUE)).apply {
            x = 10f
            y = 20f
            zIndex = 10
        }
        addActor(healthAmount)
    }

    fun updateFrom(viewModel: PersonageViewModel) {
        healthBarGreen.addAction(ScaleToAction().apply {
            setScale(viewModel.stats.health.toFloat() / viewModel.stats.maxHealth, 1f)
            duration = 0.2f
            healthAmount.setText("${viewModel.stats.health}/${viewModel.stats.maxHealth}")
        })
    }

}