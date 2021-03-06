package com.game7th.swipe.campaign.party

import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.game7th.swipe.ScreenContext

class PersonageTabView(
        private val context: ScreenContext
) : Group() {

    val bg = Image(context.uiAtlas.findRegion("ui_dialog")).apply {
        width = 480f * context.scale
        height = 170f * context.scale
    }

    init {
        addActor(bg)
    }
}