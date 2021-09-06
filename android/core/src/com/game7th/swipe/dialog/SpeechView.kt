package com.game7th.swipe.dialog

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.actions.MoveByAction
import com.badlogic.gdx.scenes.scene2d.actions.RunnableAction
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.campaign.plist.PersonageVerticalPortrait
import com.game7th.swipe.campaign.plist.PortraitConfig
import kotlinx.coroutines.launch
import ktx.actors.onClick
import ktx.async.KtxAsync

class SpeechView(
        private val context: GdxGameContext,
        private val textId: String,
        private val attachX: Float,
        private val attachY: Float,
        private val dismisser: () -> Unit
) : Group() {

    val textLabel = Label("", Label.LabelStyle(context.regularFont, Color.BLACK)).apply {
        x = attachX + wid * context.scale * 0.1f
        width = wid * context.scale * 0.8f
        height = hei * context.scale * 0.8f
        y = attachY - hei * context.scale
        setFontScale(context.scale * 100f/3f/36f)
        setAlignment(Align.topLeft)
        wrap = true
        onClick { dismiss() }
    }

    val bg = Image(context.commonAtlas.findRegion("bg_dialog")).apply {
        x = attachX
        y = attachY - context.scale * hei
        width = wid * context.scale
        height = hei * context.scale
    }

    val modale = Image(context.commonAtlas.findRegion("panel_modal_trans")).apply {
        x = 0f
        y = 0f
        width = Gdx.graphics.width.toFloat()
        height = Gdx.graphics.height.toFloat()
        onClick { }
    }

    init {
        addActor(modale)
        addActor(bg)
        addActor(textLabel)

        KtxAsync.launch {
            context.stringService.getString(textId).let { textLabel.setText(it) }
        }

    }

    private fun dismiss() {
        dismisser()
        remove()
    }
    companion object {
        const val wid = 280f
        const val hei = 130f
    }
}