package com.game7th.swipe.campaign.inventory

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.game7th.metagame.inventory.InventoryItem
import com.game7th.swipe.ScreenContext
import ktx.actors.onClick

class InventoryDetailPanel(
        private val context: ScreenContext,
        private val item: InventoryItem,
        private val action: String,
        private val dismisser: () -> Unit,
        private val equipper: (item: InventoryItem) -> Unit
) : Group() {

    val bg = Image(context.uiAtlas.findRegion("ui_dialog")).apply {
        width = context.scale * 140f
        height = context.scale * 190f
        onClick {
            dismisser()
        }
    }

    val itemView = ItemView(context, item, false).apply {
        x = 10f * context.scale
        y = 60f * context.scale
        setScale(2f)
        touchable = Touchable.disabled
    }

    val nameLabel = Label(item.name, Label.LabelStyle(context.font, Color.BLACK)).apply {
        x = 10f * context.scale
        y = 40f * context.scale
        setFontScale(18f * context.scale / 36f)
        width = 120f * context.scale
        height = 20f * context.scale
        setAlignment(Align.center)
        touchable = Touchable.disabled
    }

    val equipButton = Button(Button.ButtonStyle(TextureRegionDrawable(context.uiAtlas.findRegion("ui_button_simple")),
        TextureRegionDrawable(context.uiAtlas.findRegion("ui_button_pressed")), null)).apply {
        x = 10f * context.scale
        y = 10f * context.scale
        width = 120f * context.scale
        height = 20f * context.scale
        onClick { equipper(item) }
    }

    val equipLabel = Label(action, Label.LabelStyle(context.font, Color.BLACK)).apply {
        x = equipButton.x
        y = equipButton.y
        width = equipButton.width
        height = equipButton.height
        setFontScale(18f * context.scale / 36f)
        setAlignment(Align.center)
        touchable = Touchable.disabled
    }

    init {
        addActor(bg)
        addActor(itemView)
        addActor(nameLabel)
        addActor(equipButton)
        addActor(equipLabel)
    }
}