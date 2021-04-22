package com.game7th.swipe

import com.badlogic.gdx.Screen
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.game7th.swipe.dialog.SpeechView
import com.game7th.swipe.dialog.DismissStrategy
import com.game7th.swipe.dialog.FocusView
import com.game7th.swipe.reward.RewardCollectionView
import com.game7th.swiped.api.PackEntryDto

abstract class BaseScreen(
    protected val context: GdxGameContext,
    val game: SwipeGameGdx
) : Screen {

    lateinit protected var stage: Stage

    private var focusView: FocusView? = null
    private var focusShown = 0

    override fun show() {
        stage = Stage(ScreenViewport())
        game.multiplexer.addProcessor(0, stage)
    }

    override fun render(delta: Float) {
        stage.act(delta)
        stage.draw()
    }

    fun showDialog(portrait: String, name: String, text: String, dismisser: () -> Unit) {
        SpeechView(context, name, text, portrait, dismisser).let { dialog -> stage.addActor(dialog) }
    }

    fun showRewardDialog(title: String, rewards: List<PackEntryDto>) {
        val dialog = RewardCollectionView(context, this@BaseScreen, rewards, title)
        stage.addActor(dialog)
    }

    open fun showFocusView(text: String, rect: Rectangle, strategy: DismissStrategy, dismissCallback: (() -> Unit)? = null) {
        focusView?.let { dismissFocusView() }
        focusView = FocusView(context, rect, context.texts[text] ?: text, strategy, dismissCallback)
        stage.addActor(focusView)
        focusShown++
    }

    open fun dismissFocusView() {
        focusView?.let {
            it.remove()
            focusView = null
            it.forceDismiss()
        }
        focusShown = 0
    }

    protected fun isFocusShown() = focusShown > 0

    override fun resize(width: Int, height: Int) {}

    override fun pause() {}

    override fun resume() {}

    override fun hide() {}

    override fun dispose() {}

    open fun currencyUpdated() {
    }

    open fun inventoryUpdated() {}

    open fun personagesUpdated() {}
}