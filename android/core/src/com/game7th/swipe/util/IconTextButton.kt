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
            TextureRegionDrawable(context.commonAtlas.findRegion("ui_button_trans_simple")),
            TextureRegionDrawable(context.commonAtlas.findRegion("ui_button_trans_pressed")),
            null)).apply {
        width = context.scale * 120f
        height = context.scale * 48f
    }

    val icon = Image(context.commonAtlas.findRegion(iconTexture)).apply {
        x = btn.x + context.scale * 8f
        y = btn.y + context.scale * 8f
        width = context.scale * 32f
        height = context.scale * 32f
        touchable = Touchable.disabled
    }

    val labelForge = Label(labelText, Label.LabelStyle(context.regularFont, Color.WHITE)).apply {
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