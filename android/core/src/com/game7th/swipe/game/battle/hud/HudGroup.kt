package com.game7th.swipe.game.battle.hud

import com.badlogic.gdx.scenes.scene2d.Group
import com.game7th.swipe.GdxGameContext
import com.game7th.swiped.api.battle.PersonageViewModel

class HudGroup(private val context: GdxGameContext) : Group() {

    fun showHud(x: Int, personage: PersonageViewModel) {
        val personageHud = PersonageHud(context, personage, 88f).apply {
            this.x = 5f + x * 88f
        }
        addActor(personageHud)
    }

    fun removeHud(id: Int) {
        findActor<PersonageHud>(id.toString()).apply {
            remove()
        }
    }

    fun updateHud(personage: PersonageViewModel) {
        findActor<PersonageHud>(personage.id.toString())?.let {
            it.updateSelf(personage)
        }

    }

}