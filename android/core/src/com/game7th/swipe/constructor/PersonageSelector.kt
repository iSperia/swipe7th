package com.game7th.swipe.constructor

import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.game7th.swipe.GdxGameContext
import ktx.actors.onClick

class PersonageSelector(
        val skins: List<String>,
        val context: GdxGameContext,
        callback: (Int) -> Unit
) : Group() {

    val bg = Image(context.atlas.createPatch("ui_dialog")).apply {
        width = 370f
        height = 370f
    }

    init {
        addActor(bg)

        skins.withIndex().forEach {
            val skin = Image(context.atlas.findRegion(it.value)).apply {
                width = 60f
                height = 120f
                x = (it.index % 6) * 60f
                y = 360f - (1 + it.index / 6) * 120f
            }

            addActor(skin)

            skin.onClick {
                callback(it.index)
            }
        }
    }

}