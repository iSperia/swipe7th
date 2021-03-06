package com.game7th.swipe.campaign.plist

import com.badlogic.gdx.scenes.scene2d.Group
import com.game7th.metagame.unit.UnitConfig
import com.game7th.swipe.ScreenContext
import ktx.actors.onClick

class PersonageScrollActor(
        private val context: ScreenContext,
        private val personages: List<UnitConfig>,
        private val h: Float,
        private val indexSelectable: Boolean,
        private val defaultIndex: Int = 0
) : Group() {

    private val ratio = 1 / 1.5f
    private val elementWidth = h * ratio
    private val lvlSize = elementWidth * 0.25f
    private val lvlPadding = lvlSize * 0.25f

    var selectionCallback: ((Int) -> Unit)? = null

    var selectedIndex = if (indexSelectable) defaultIndex else -1

    init {
        width = elementWidth * personages.size
        height = h

        personages.forEachIndexed { index, unitConfig ->
            val groupX = index * elementWidth
            val portrait = PersonageVerticalPortrait(context, unitConfig, h).apply {
                x = groupX
            }
            addActor(portrait)

            portrait.onClick {
                if (indexSelectable) {
                    selectedIndex = index
                    applySelection()
                }
            }
        }

        applySelection()
    }

    private fun applySelection() {
        children.withIndex().forEach {
            val portrait = it.value as PersonageVerticalPortrait
            portrait.setFocused(it.index == selectedIndex)
        }
    }
}