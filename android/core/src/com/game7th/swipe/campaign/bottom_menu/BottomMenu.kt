package com.game7th.swipe.campaign.bottom_menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.game7th.swipe.ScreenContext

class BottomMenu(context: ScreenContext) : Group() {

    val bg = Image(context.uiAtlas.findRegion("ui_dialog")).apply {
        x = 0f
        y = 0f
        width = Gdx.graphics.width.toFloat()
        height = context.scale * 48f
    }

    val btnSquads = Button(Button.ButtonStyle(
            TextureRegionDrawable(context.uiAtlas.findRegion("ui_button_trans_simple")),
            TextureRegionDrawable(context.uiAtlas.findRegion("ui_button_trans_pressed")),
            null)).apply {
        x = 12f
        y = 0f
        width = context.scale * 144f
        height = context.scale * 48f
    }

    val iconSquads = Image(context.uiAtlas.findRegion("icon_squads")).apply {
        x = btnSquads.x
        y = btnSquads.y
        width = context.scale * 48f
        height = context.scale * 48f
        touchable = Touchable.disabled
    }

    val labelSquads = Label("Party", Label.LabelStyle(context.font, Color.BLACK)).apply {
        x = btnSquads.x + context.scale * 48f
        y = btnSquads.y + context.scale * 12f
        setAlignment(Align.center)
        width = 96f * context.scale
        height = 24f * context.scale
        touchable = Touchable.disabled
    }

    init {
        addActor(bg)

        addActor(btnSquads)
        addActor(iconSquads)
        addActor(labelSquads)
    }
}