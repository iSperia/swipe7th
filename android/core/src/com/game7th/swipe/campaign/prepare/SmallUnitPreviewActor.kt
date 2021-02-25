package com.game7th.swipe.campaign.prepare

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.game7th.metagame.unit.UnitConfig
import com.game7th.swipe.ScreenContext

/**
 * Small unit preview
 */
class SmallUnitPreviewActor(
        context: ScreenContext,
        unit: UnitConfig
) : Group() {

    private val imagePreview: Image = Image(context.battleAtlas.findRegion(unit.unitType.getPortrait())).apply {
        width = 120 * context.scale
        height = 60 * context.scale
    }
    private val levelLabel: Label

    init {
        addActor(imagePreview)

        levelLabel = Label("LVL ${unit.level}", Label.LabelStyle(context.font, Color.BLUE)).apply {
            height = 16 * context.scale
            setAlignment(Align.right)
            setFontScale(0.5f * context.scale)
            x = 70 * context.scale
            y = 5 * context.scale
        }
        addActor(levelLabel)
    }
}