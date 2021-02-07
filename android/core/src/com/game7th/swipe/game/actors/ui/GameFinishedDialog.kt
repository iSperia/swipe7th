package com.game7th.swipe.game.actors.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.game7th.swipe.GdxGameContext

class GameFinishedDialog(
        private val context: GdxGameContext,
        private val text: String,
        callback: () -> Unit
        ) : Group() {

    val background = Image(context.atlas.createPatch("ui_dialog")).apply {
        width = 400f
        height = 300f
        zIndex = 5
        addActor(this)
    }

    val buttonClose = Image(context.atlas.createPatch("ui_button")).apply {
        width = 120f
        height = 20f
        zIndex = 6
        x = 140f
        y = 20f
        addActor(this)

        addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                super.clicked(event, x, y)
                callback()
            }
        })
    }

    val buttonCloseLabel = Label("CLOSE", Label.LabelStyle(context.font, Color.WHITE)).apply {
        width = 120f
        height = 20f
        zIndex = 7
        x = 140f
        y = 20f
        setAlignment(Align.center)
        addActor(this)
        touchable = Touchable.disabled
    }

    val label = Label(text, Label.LabelStyle(context.font, Color.RED)).apply {
        setAlignment(Align.center)
        width = 400f
        height = 100f
        setFontScale(4f)
        x = 0f
        y = 150f
        zIndex = 10
        addActor(this)
    }
}