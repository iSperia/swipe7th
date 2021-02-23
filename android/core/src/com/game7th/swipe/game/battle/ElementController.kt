package com.game7th.swipe.game.battle

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.game7th.swipe.game.GameContextWrapper
import com.game7th.swipe.game.battle.model.BattleControllerEvent

abstract class ElementController(
        protected val context: GameContextWrapper,
        public val id: Int
) {

    abstract fun render(batch: SpriteBatch, delta: Float)

    open fun dispose() {}

    abstract fun handle(event: BattleControllerEvent)
}