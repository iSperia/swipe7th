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

    private val imagePreview: Image = Image(context.battleAtlas.findRegion(unit.unitType.getSkin())).apply {
        y = 12 * context.scale
        width = 30 * context.scale
        height = 60 * context.scale
    }
    private val levelLabel: Label

    init {
        addActor(imagePreview)

        levelLabel = Label("LVL ${unit.level}", Label.LabelStyle(context.font, Color.WHITE)).apply {
            height = 12 * context.scale
            width = 30 * context.scale
            setAlignment(Align.center)
            setFontScale(9f / 15 * context.scale)
        }
        addActor(levelLabel)
    }
}