package com.game7th.swipe.game.battle

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.game7th.battle.event.BattleEvent
import com.game7th.swipe.game.GameContextWrapper

/**
 * GDX graph controller for battle
 */
class BattleController(
        private val context: GameContextWrapper,
        private val y: Float
) {

    /**
     * events to resolve
     */
    private val eventQueue = mutableListOf<BattleEvent>()

    /**
     * As soon as lock is zero, we are ready to process next event.
     * Use orchestrators to reduce event lock
     */
    private val eventProcessLock = 0

    private val controllers = mutableListOf<ElementController>()

    private val backgroundTexture = context.gameContext.atlas.findRegion("battle_bg", 1)

    fun enqueueEvent(event: BattleEvent) {
        eventQueue.add(event)
    }

    fun act(batch: SpriteBatch, delta: Float) {
        batch.draw(backgroundTexture, 0f, y, context.width, context.width * 0.67f)

        controllers.forEach {
            it.render(batch, delta)
        }

        if (eventProcessLock == 0 && eventQueue.isNotEmpty()) {
            val event = eventQueue.removeAt(0)
            //ok, we have an event
            processEvent(event)
        }
    }

    private val paddingSide = context.width * 0.05f

    private fun processEvent(event: BattleEvent) {
        when (event) {
            is BattleEvent.CreatePersonageEvent -> {
                val figure = FigureController(context,
                        context.gdxModel.figure(event.personage.skin) ?: context.gdxModel.figure("personage_slime")!!,
                        paddingSide + (context.width - 2 * paddingSide) * 0.2f * (0.5f + event.position),
                        y,
                        context.gameContext.scale * 0.8f,
                        event.personage.team > 0)
                controllers.add(figure)
            }
        }
    }
}
