package com.game7th.swipe.network

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.utils.Align
import com.game7th.swipe.BaseScreen
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.SwipeGameGdx
import ktx.actors.onClick

class NetworkErrorScreen(
        context: GdxGameContext,
        game: SwipeGameGdx,
        val errorMessage: String,
        val quitCallback: () -> Unit
) : BaseScreen(context, game) {

    val panel = Image(context.commonAtlas.createPatch("ui_hor_panel")).apply {
        x = 40f * context.scale
        y = (Gdx.graphics.height - 200f * context.scale) / 2f
        width = 400f * context.scale
        height = 200f * context.scale
        touchable = Touchable.disabled
    }

    val labelError = Label(errorMessage, Label.LabelStyle(context.regularFont, Color.RED)).apply {
        x = panel.x
        y = panel.y + 150f * context.scale
        width = 400f * context.scale
        height = 30f * context.scale
        setFontScale(20f * context.scale / 36f)
        setAlignment(Align.center)
    }

    val labelQuit = Label("Quit", Label.LabelStyle(context.regularFont, Color.WHITE)).apply {
        x = panel.x + 50f * context.scale
        y = panel.y + 10f * context.scale
        width = 125f * context.scale
        height = 30f * context.scale
        setAlignment(Align.center)
        setFontScale(30f * context.scale / 36f)
        touchable = Touchable.disabled
    }

    val buttonQuit = Button(Button.ButtonStyle(
            NinePatchDrawable(context.commonAtlas.createPatch("ui_button_simple")),
            NinePatchDrawable(context.commonAtlas.createPatch("ui_button_pressed")), null)).apply {
        x = labelQuit.x
        y = labelQuit.y
        width = labelQuit.width
        height = labelQuit.height
    }

    override fun show() {
        super.show()
        stage.addActor(panel)
        stage.addActor(labelError)
        stage.addActor(buttonQuit)
        stage.addActor(labelQuit)

        buttonQuit.onClick { quitCallback() }
    }

}