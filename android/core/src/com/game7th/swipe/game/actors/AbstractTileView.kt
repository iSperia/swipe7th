package com.game7th.swipe.game.actors

import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.Group
import com.game7th.swiped.api.battle.TileViewModel

abstract class AbstractTileView : Group() {

    var tx: Int = 0
    var ty: Int = 0
    val durationActionQueue = mutableListOf<Action>()
    var removed = false

    abstract fun updateFrom(vm: TileViewModel)

    override fun act(delta: Float) {
        super.act(delta)

        if (!hasActions() && durationActionQueue.isNotEmpty()) {
            addAction(durationActionQueue.removeAt(0))
        }
    }
}