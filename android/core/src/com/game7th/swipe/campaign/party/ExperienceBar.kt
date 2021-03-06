package com.game7th.swipe.campaign.party

import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.actions.ScaleToAction
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.game7th.swipe.ScreenContext
import kotlin.math.min

class ExperienceBar(
        private val context: ScreenContext,
        private val w: Float,
        private val h: Float,
        private val value: Int,
        private val maxValue: Int
) : Group() {

    val padding = 0.08f * h

    val bg = Image(context.uiAtlas.findRegion("ui_exp_bar_bg")).apply {
        width = w
        height = h
    }

    val fg = Image(context.uiAtlas.findRegion("ui_exp_bar_fg")).apply {
        x = padding
        y = padding
        width = w - 2 * padding
        height = h - 2 * padding
        scaleX = min(1f, value.toFloat() / maxValue)
    }

    init {
        addActor(bg)
        addActor(fg)
        fg.scaleX = 0f
        fg.addAction(ScaleToAction().apply {
            setScale(min(1f, value.toFloat() / maxValue), 1f)
            duration = 0.2f
        })
    }
}
