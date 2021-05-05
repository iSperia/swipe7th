package com.game7th.swipe.game.actors

import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.game7th.swipe.GdxGameContext
import ktx.actors.onClick

class BigButtonGroup(
        private val context: GdxGameContext,
        private val w: Float,
        private val h: Float,
        private val textureBackground: String,
        private val textureBackgroundPressed: String,
        private val textureForeground: String,
        private val textureIcon: String,
        private val textureIconPressed: String,
        private val alignment: Int,
        click: () -> Unit
) : Group() {

    val background = Image(context.battleAtlas.findRegion(textureBackground)).apply {
        width = w
        height = h
    }
    private val icon = Button(TextureRegionDrawable(context.battleAtlas.findRegion(textureIcon)), TextureRegionDrawable(context.battleAtlas.findRegion(textureIconPressed))).apply {
        width = 40f * context.scale
        height = 40f * context.scale
        x = if (alignment == Align.right) (w - 50f * context.scale) else 10f * context.scale
        y = 12f * context.scale
    }

    private val foreground = Image(context.battleAtlas.findRegion(textureForeground)).apply {
        width = w
        height = h
        touchable = Touchable.disabled
    }

    init {
        addActor(background)
        addActor(icon)
        addActor(foreground)

        icon.addListener(object : ClickListener() {
            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                background.drawable = TextureRegionDrawable(context.battleAtlas.findRegion(textureBackgroundPressed))
                return super.touchDown(event, x, y, pointer, button)
            }

            override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
                background.drawable = TextureRegionDrawable(context.battleAtlas.findRegion(textureBackground))
                return super.touchUp(event, x, y, pointer, button)
            }
        })
        icon.onClick {
            click()
        }
    }
}