package com.game7th.swipe.campaign.bottom_menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.util.IconTextButton

class BottomMenu(
        context: GdxGameContext
) : Group() {

    var onPartyButtonPressed: (() -> Unit)? = null
    var onShopButtonPressed: (() -> Unit)? = null
    var onAlchButtonPressed: (() -> Unit)? = null

    val bg = Image(context.commonAtlas.createPatch("ui_hor_panel")).apply {
        x = 0f
        y = 0f
        width = Gdx.graphics.width.toFloat()
        height = context.scale * 48f
    }

    val buttonSquads = IconTextButton(context, "icon_squads", context.texts["button_party"]!!) {
        onPartyButtonPressed?.invoke()
    }

    val buttonShop = IconTextButton(context, "icon_shop", context.texts["button_shop"]!!) {
        onShopButtonPressed?.invoke()
    }.apply {
        x = buttonSquads.x + buttonSquads.width
    }

    val buttonAlch = IconTextButton(context, "icon_alch", context.texts["button_lab"]!!) {
        onAlchButtonPressed?.invoke()
    }.apply {
        x = buttonShop.x + buttonShop.width
    }

    init {
        addActor(bg)

        addActor(buttonSquads)
        addActor(buttonShop)
        addActor(buttonAlch)
    }
}