package com.game7th.swipe.util

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.game7th.swipe.GdxGameContext
import ktx.actors.onClick

class IconTextButton(
        private val context: GdxGameContext,
        private val iconTexture: String,
        private val labelText: String,
        private val onClick: (() -> Unit)
): Group() {

    val btn = Button(Button.ButtonStyle(
            TextureRegionDrawable(context.uiAtlas.findRegion("ui_button_trans_simple")),
            TextureRegionDrawable(context.uiAtlas.findRegion("ui_button_trans_pressed")),
            null)).apply {
        width = context.scale * 120f
        height = context.scale * 48f
    }

    val icon = Image(context.uiAtlas.findRegion(iconTexture)).apply {
        x = btn.x
        y = btn.y
        width = context.scale * 48f
        height = context.scale * 48f
        touchable = Touchable.disabled
    }

    val labelForge = Label(labelText, Label.LabelStyle(context.font, Color.BLACK)).apply {
        x = btn.x + context.scale * 52f
        y = btn.y + context.scale * 12f
        setAlignment(Align.left)
        width = 96f * context.scale
        height = 24f * context.scale
        touchable = Touchable.disabled
        setFontScale(20f * context.scale / 36f)
    }

    init {
        addActor(btn)
        addActor(icon)
        addActor(labelForge)

        btn.onClick { onClick() }

        width = btn.width
        height = btn.height
    }
}