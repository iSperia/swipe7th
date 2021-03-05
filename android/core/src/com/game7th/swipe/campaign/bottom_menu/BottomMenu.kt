package com.game7th.swipe.campaign.bottom_menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.game7th.swipe.ScreenContext

class BottomMenu(context: ScreenContext) : Group() {

    val bg = Image(context.uiAtlas.findRegion("ui_dialog")).apply {
        x = 0f
        y = 0f
        width = Gdx.graphics.width.toFloat()
        height = context.scale * 48f
    }

    init {
        addActor(bg)
    }
}