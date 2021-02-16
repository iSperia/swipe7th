package com.game7th.swipe.campaign.prepare

import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.game7th.metagame.unit.SquadConfig
import com.game7th.swipe.ScreenContext
import ktx.actors.onClick

interface SquadBrowserAdapter {
    fun count(): Int
    fun getSquad(index: Int): SquadConfig
}

class SquadBrowserActor(
        private val context: ScreenContext,
        private val adapter: SquadBrowserAdapter
) : Group() {

    private val background: Image
    private var activeSquad: SquadPreviewActor? = null
    private val btnPrev: Button
    private val btnNext: Button

    var index = 0

    init {
        background = Image(context.uiAtlas.createPatch("panel_blue")).apply {
            width = 160 * context.scale
            height = 105 * context.scale
        }
        background.touchable = Touchable.disabled
        addActorAt(0, background)

        btnPrev = Button(Button.ButtonStyle(TextureRegionDrawable(context.uiAtlas.findRegion("arrowBlue_left")), null, null)).apply {
            width = context.scale * 24
            height = context.scale * 24
            x = 2 * context.scale
            y = 40 * context.scale
        }
        btnNext = Button(Button.ButtonStyle(TextureRegionDrawable(context.uiAtlas.findRegion("arrowBlue_right")), null, null)).apply {
            width = context.scale * 24
            height = context.scale * 24
            x = 134 * context.scale
            y = 40 * context.scale
        }
        addActor(btnPrev)
        addActor(btnNext)

        updateSquad()
        btnPrev.onClick {
            index--
            if (index < 0) index = adapter.count() - 1
            updateSquad()
        }
        btnNext.onClick {
            index++
            if (index >= adapter.count()) index = 0
            updateSquad()
        }
    }

    private fun updateSquad() {
        activeSquad?.let {
            it.remove()
        }
        activeSquad = SquadPreviewActor(context, adapter.getSquad(index)).apply {
            x = context.scale * 12
        }
        addActorAt(1, activeSquad)
        activeSquad?.touchable = Touchable.disabled
    }

    fun update() {
        updateSquad()
    }
}