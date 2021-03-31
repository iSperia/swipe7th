package com.game7th.swipe.campaign.inventory

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.game7th.metagame.inventory.dto.FlaskStackDto
import com.game7th.metagame.inventory.dto.InventoryItem
import com.game7th.swipe.GdxGameContext

sealed class ItemViewAdapter {
    data class InventoryItemAdapter(val item: InventoryItem): ItemViewAdapter() {
        override fun getIcon() = "art_${item.name}"
        override fun getLabel() = "${item.level}"
        override fun getName() = item.name
    }

    data class PotionItemAdater(val potion: FlaskStackDto): ItemViewAdapter() {
        override fun getIcon() = "${potion.template.name.toLowerCase()}"
        override fun getLabel() = "x${potion.amount}"
        override fun getName() = potion.template.name
    }

    object EmptyAdapter: ItemViewAdapter() {
        override fun getIcon() = null
        override fun getLabel() = ""
        override fun getName() = ""
    }

    data class PackItemAdapter(val packName: String): ItemViewAdapter() {
        override fun getName() = packName
        override fun getIcon() = packName.toLowerCase()
        override fun getLabel() = "PACK!"
    }

    abstract fun getName(): String
    abstract fun getIcon(): String?
    abstract fun getLabel(): String
}

class ItemView(
        private val context: GdxGameContext,
        val item: ItemViewAdapter,
        backgroundShown: Boolean,
        val size: Float = context.scale * 60f
) : Group() {

    val bg = Image(context.uiAtlas.findRegion("ui_item_bg")).apply {
        width = size
        height = size
        isVisible = backgroundShown
    }

    val image: Image? = item.getIcon()?.let {
        Image(context.battleAtlas.findRegion(it)).apply {
            width = size
            height = size
        }
    }

    val lvlLabel = Label(item.getLabel(), Label.LabelStyle(context.font, Color.WHITE)).apply {
        x = 25f * context.scale
        y = 5f
        width = size / 2
        height = size / 2
        setAlignment(Align.bottomRight)
        setFontScale(size / 3 / 36f)
    }

    init {
        addActor(bg)
        image?.let { addActor(it) }
        addActor(lvlLabel)

        width = size
        height = size
    }
}