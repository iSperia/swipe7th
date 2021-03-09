package com.game7th.swipe.dialog

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.actions.MoveByAction
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.game7th.swipe.ScreenContext
import com.game7th.swipe.campaign.plist.PersonageVerticalPortrait
import com.game7th.swipe.campaign.plist.PortraitConfig
import ktx.actors.onClick

class DialogView(
        private val context: ScreenContext,
        name: String,
        text: String,
        texture: String,
        private val dismisser: () -> Unit
) : Group() {

    val portrait = PersonageVerticalPortrait(context, PortraitConfig(name, texture, -1), 200f * context.scale).apply {
        onClick { dismiss() }
    }

    val textLabel = Label(text, Label.LabelStyle(context.font, Color.BLACK)).apply {
        x = 140f * context.scale
        width = 330f * context.scale
        height = 180f * context.scale
        y = 10f * context.scale
        setFontScale(200f/3f/36f)
        setAlignment(Align.topLeft)
        wrap = true
        onClick { dismiss() }
    }

    val bg = Image(context.uiAtlas.findRegion("ui_dialog")).apply {
        x = 130f * context.scale
        width = 350f * context.scale
        height = 200f * context.scale
    }

    val modale = Image(context.uiAtlas.findRegion("panel_modal")).apply {
        x = 0f
        y = 0f
        width = Gdx.graphics.width.toFloat()
        height = Gdx.graphics.height.toFloat() + 200f * context.scale
        onClick { }
    }

    init {
        addActor(modale)
        addActor(portrait)
        addActor(bg)
        addActor(textLabel)

        y = -200f * context.scale
        addAction(MoveByAction().apply { duration = 0.8f; setAmount(0f, 200f * context.scale) })
    }

    private fun dismiss() {
        dismisser()
        remove()
    }
}