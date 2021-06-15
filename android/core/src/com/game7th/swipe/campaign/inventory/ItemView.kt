package com.game7th.swipe.campaign.inventory

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.game7th.swipe.GdxGameContext
import com.game7th.swiped.api.FlaskItemFullInfoDto
import com.game7th.swiped.api.InventoryItemFullInfoDto

sealed class ItemViewAdapter {
    data class InventoryItemAdapter(val item: InventoryItemFullInfoDto): ItemViewAdapter() {
        override fun getIcon() = "art_${item.template.name}"
        override fun getLabel() = "${item.level}"
        override fun getName() = item.template.name
    }

    data class PotionItemAdater(val potion: FlaskItemFullInfoDto): ItemViewAdapter() {
        override fun getIcon() = "${potion.template.name.toLowerCase()}"
        override fun getLabel() = "${potion.stackSize}"
        override fun getName() = potion.template.name
    }

    object EmptyAdapter: ItemViewAdapter() {
        override fun getIcon() = null
        override fun getLabel() = ""
        override fun getName() = ""
    }

    data class PackItemAdapter(val packId: String, val texture: String, val packName: String): ItemViewAdapter() {
        override fun getName() = packName
        override fun getIcon() = texture
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
        Image(context.uiAtlas.findRegion(it) ?: context.battleAtlas.findRegion(it)).apply {
            width = ITEM_SIZE * context.scale
            height = ITEM_SIZE * context.scale
            x = (size - width) / 2f
            y = (size - height) / 2f
        }
    }

    val lvlLabel = Label(item.getLabel(), Label.LabelStyle(context.captionFont, Color.WHITE)).apply {
        width = textSize * size
        height = textSize * size
        setAlignment(Align.bottomLeft)
        setFontScale(textSize * size / 36f)
        touchable = Touchable.disabled
    }

    init {
        addActor(bg)
        image?.let { addActor(it) }
        addActor(lvlLabel)

        width = size
        height = size
    }

    companion object {
        val ITEM_SIZE = 64f
        val textSize = 0.4f
    }
}