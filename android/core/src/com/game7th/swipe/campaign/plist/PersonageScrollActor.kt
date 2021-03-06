package com.game7th.swipe.campaign.plist

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.game7th.metagame.unit.UnitConfig
import com.game7th.swipe.ScreenContext
import ktx.actors.onClick

class PersonageScrollActor(
        private val context: ScreenContext,
        private val personages: List<UnitConfig>,
        private val h: Float,
        private val indexSelectable: Boolean
) : Group() {

    private val ratio = 1 / 1.5f
    private val elementWidth = h * ratio
    private val lvlSize = elementWidth * 0.25f
    private val lvlPadding = lvlSize * 0.25f

    var selectedIndex = if (indexSelectable) 0 else -1

    init {
        width = elementWidth * personages.size
        height = h

        personages.forEachIndexed { index, unitConfig ->
            val groupX = index * elementWidth
            val bg = Image(context.battleAtlas.findRegion("vp_${unitConfig.unitType.getSkin()}")).apply {
                x = groupX
                width = elementWidth
                height = h
            }
            val fg = Image(context.uiAtlas.findRegion("ui_portrait_fg")).apply {
                x = groupX
                width = elementWidth
                height = h
            }
            val lvl = Image(context.uiAtlas.findRegion("ui_level_bg")).apply {
                x = groupX + lvlPadding
                y = h - lvlSize - lvlPadding
                width = lvlSize
                height = lvlSize
                touchable = Touchable.disabled
            }
            val lvlLabel = Label(unitConfig.level.toString(), Label.LabelStyle(context.font, Color.YELLOW)).apply {
                x = groupX + lvlPadding
                y = h - lvlSize - lvlPadding
                width = lvlSize
                height = lvlSize
                setAlignment(Align.center)
                setFontScale(0.7f * lvlSize / 36f)
                touchable = Touchable.disabled
            }
            val name = Label(unitConfig.unitType.toString(), Label.LabelStyle(context.font, Color.BLACK)).apply {
                x = groupX + lvlPadding
                y = 0.03f * h
                width = elementWidth - 2 * lvlPadding
                height = 0.11f * h
                touchable = Touchable.disabled
                setAlignment(Align.center)
                setFontScale(0.09f * h / 36f)
            }

            fg.onClick {
                if (indexSelectable) {
                    selectedIndex = index
                    applySelection()
                }
            }
            addActor(bg)
            addActor(fg)
            addActor(lvl)
            addActor(lvlLabel)
            addActor(name)
        }

        applySelection()
    }

    private fun applySelection() {
        children.withIndex().filter { it.index % 5 == 1 }.map { it.value }.forEachIndexed { index, fg ->
            (fg as? Image)?.let { image ->
                image.drawable = TextureRegionDrawable(context.uiAtlas.findRegion(if (index == selectedIndex) "ui_portrait_fg_focused" else "ui_portrait_fg"))
            }
        }
    }
}