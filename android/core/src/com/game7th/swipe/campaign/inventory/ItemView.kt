package com.game7th.swipe.campaign.inventory

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.game7th.metagame.inventory.InventoryItem
import com.game7th.swipe.ScreenContext

class ItemView(
        private val context: ScreenContext,
        private val item: InventoryItem?,
        backgroundShown: Boolean
) : Group() {

    init {
        println(item?.name)
    }

    val bg = Image(context.uiAtlas.findRegion("ui_item_bg")).apply {
        width = 60f * context.scale
        height = 60f * context.scale
        isVisible = backgroundShown
    }

    val image: Image? = item?.name?.let {
        Image(context.battleAtlas.findRegion("art_${it}")).apply {
            width = 60f * context.scale
            height = 60f * context.scale
        }
    }

    val lvlLabel = Label(item?.level?.toString() ?: "", Label.LabelStyle(context.font, Color.WHITE)).apply {
        x = 25f * context.scale
        y = 5f
        width = 30f * context.scale
        height = 30f * context.scale
        setAlignment(Align.bottomRight)
        setFontScale(20f * context.scale / 36f)
    }

    init {
        addActor(bg)
        image?.let { addActor(it) }
        addActor(lvlLabel)

        width = 60f * context.scale
        height = 60f * context.scale
    }
}