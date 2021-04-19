package com.game7th.swipe.campaign.inventory

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.game7th.metagame.inventory.dto.FlaskStackDto
import com.game7th.swipe.GdxGameContext
import com.game7th.swiped.api.InventoryItemFullInfoDto

sealed class ItemViewAdapter {
    data class InventoryItemAdapter(val item: InventoryItemFullInfoDto): ItemViewAdapter() {
        override fun getIcon() = "art_${item.template.name}"
        override fun getLabel() = "${item.level}"
        override fun getName() = item.template.name
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

    data class PackItemAdapter(val packId: String, val packName: String): ItemViewAdapter() {
        override fun getName() = packName
        override fun getIcon() = "inapp_${packId.replace("_", "")}"
        override fun getLabel() = ""
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
        Image(context.uiAtlas.findRegion(it)).apply {
            width = size * 0.9f
            height = size * 0.9f
            x = size * 0.05f
            y = size * 0.05f
        }
    }

    val lvlLabel = Label(item.getLabel(), Label.LabelStyle(context.font, Color.WHITE)).apply {
        x = size * 0.5f
        y = size * 0.1f
        width = size / 3
        height = size / 3
        setAlignment(Align.bottomRight)
        setFontScale(size / 3 / 36f)
    }

    val lvlLabelShadow = Label(item.getLabel(), Label.LabelStyle(context.font, Color.BLACK)).apply {
        x = lvlLabel.x + 1f * context.scale
        y = lvlLabel.y - 1f * context.scale
        width = lvlLabel.width
        height = lvlLabel.height
        setAlignment(Align.bottomRight)
        setFontScale(size / 3 / 36f)
    }

    init {
        addActor(bg)
        image?.let { addActor(it) }
        addActor(lvlLabelShadow)
        addActor(lvlLabel)

        width = size
        height = size
    }
}