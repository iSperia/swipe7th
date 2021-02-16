package com.game7th.swipe.campaign.prepare

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.game7th.metagame.unit.SquadConfig
import com.game7th.swipe.ScreenContext

/**
 * Actor for displaying some squad stuff
 */
class SquadPreviewActor(
        private val context: ScreenContext,
        private val config: SquadConfig
) : Group() {

    val unitsGroup = Group()
    val squadNameLabel: Label

    init {
        addActor(unitsGroup)
        config.units.withIndex().forEach { (index, unit) ->
            val unitActor = SmallUnitPreviewActor(context, unit).apply {
                x = context.scale * 30 * index
            }
            unitsGroup.addActor(unitActor)
        }

        squadNameLabel = Label(config.name, Label.LabelStyle(context.font, Color.WHITE)).apply {
            y = 80f * context.scale
            x = 15f * context.scale
            width = 90f * context.scale
            height = 12f * context.scale
            setFontScale(12f / 15 * context.scale)
            setAlignment(Align.center)
        }
        addActor(squadNameLabel)
    }
}