package com.game7th.swipe.campaign.inventory

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.game7th.metagame.inventory.dto.InventoryItem
import com.game7th.swipe.GdxGameContext

class ItemView(
        private val context: GdxGameContext,
        val item: InventoryItem?,
        backgroundShown: Boolean,
        val size: Float = context.scale * 60f
) : Group() {

    val bg = Image(context.uiAtlas.findRegion("ui_item_bg")).apply {
        width = size
        height = size
        isVisible = backgroundShown
    }

    val image: Image? = item?.name?.let {
        Image(context.battleAtlas.findRegion("art_${it}")).apply {
            width = size
            height = size
        }
    }

    val lvlLabel = Label(item?.level?.toString() ?: "", Label.LabelStyle(context.font, Color.WHITE)).apply {
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