package com.game7th.swipe

import com.badlogic.gdx.Screen
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.game7th.swipe.dialog.SpeechView
import com.game7th.swipe.dialog.DismissStrategy
import com.game7th.swipe.dialog.FocusView

abstract class BaseScreen(
    protected val context: GdxGameContext
) : Screen {

    lateinit protected var stage: Stage

    private var focusView: FocusView? = null
    private var focusShown = 0

    override fun show() {
        stage = Stage(ScreenViewport())
    }

    override fun render(delta: Float) {
        stage.act(delta)
        stage.draw()
    }

    fun showDialog(portrait: String, name: String, text: String, dismisser: () -> Unit) {
        SpeechView(context, name, text, portrait, dismisser).let { dialog -> stage.addActor(dialog) }
    }

    open fun showFocusView(text: String, rect: Rectangle, strategy: DismissStrategy, dismissCallback: (() -> Unit)? = null) {
        focusView?.let { dismissFocusView() }
        focusView = FocusView(context, rect, context.texts[text] ?: text, strategy, dismissCallback)
        stage.addActor(focusView)
        focusShown++
    }

    fun dismissFocusView() {
        focusView?.let {
            it.remove()
            focusView = null
            it.forceDismiss()
        }
        focusShown--
    }

    protected fun isFocusShown() = focusShown > 0
}