package com.game7th.swipe.campaign.plist

import com.badlogic.gdx.scenes.scene2d.Group
import com.game7th.metagame.dto.UnitConfig
import com.game7th.swipe.GdxGameContext
import kotlinx.coroutines.launch
import ktx.actors.onClick
import ktx.async.KtxAsync

class PersonageScrollActor(
        private val context: GdxGameContext,
        private var personages: List<UnitConfig>,
        private val h: Float,
        private val indexSelectable: Boolean,
        private val defaultIndex: Int = 0
) : Group() {

    private val ratio = 1 / 1.5f
    private val elementWidth = h * ratio

    var selectionCallback: (suspend (Int) -> Unit)? = null

    var selectedIndex = if (indexSelectable) defaultIndex else -1

    init {
        width = elementWidth * personages.size
        height = h

        addPersonages()
    }

    fun changePersonages(personages: List<UnitConfig>) {
        this.personages = personages
        addPersonages()
    }

    private fun addPersonages() {
        children.forEach { it.remove() }
        personages.forEachIndexed { index, unitConfig ->
            val groupX = index * elementWidth
            val portrait = PersonageVerticalPortrait(context, unitConfig.toPortraitConfig(), h).apply {
                x = groupX
            }
            addActor(portrait)

            portrait.onClick {
                if (indexSelectable) {
                    selectedIndex = index
                    applySelection()
                    KtxAsync.launch { selectionCallback?.invoke(index) }
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