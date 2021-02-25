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
        val size = config.units.size
        config.units.withIndex().forEach { (index, unit) ->
            val unitActor = SmallUnitPreviewActor(context, unit).apply {
                y = ( 180f - 60f * index ) * context.scale
            }
            unitsGroup.addActor(unitActor)
        }

        squadNameLabel = Label(config.name, Label.LabelStyle(context.font, Color.WHITE)).apply {
            y = 240f * context.scale
            x = 0f
            width = 120f * context.scale
            height = 30f * context.scale
            setFontScale(32f / 30f)
            setAlignment(Align.center)
        }
        addActor(squadNameLabel)
    }
}