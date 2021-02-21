package com.game7th.swipe.game.battle

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.game7th.battle.event.BattleEvent
import com.game7th.swipe.game.GameContextWrapper
import com.game7th.swipe.game.battle.model.FigureGdxModel
import com.game7th.swipe.game.battle.model.PoseGdxModel

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

    private var c1 = false

    private fun processEvent(event: BattleEvent) {
        when (event) {
            is BattleEvent.CreatePersonageEvent -> {
                if (!c1) {
                    c1 = true
                    val figure = FigureController(context,
                            FigureGdxModel(
                                    "body_gladiator",
                                    "personage_gladiator",
                                    listOf(
                                            PoseGdxModel("idle", 26, 51)
                                    )
                            ),
                            100f,
                            y,
                             context.gameContext.scale)
                    controllers.add(figure)
                }
            }
        }
    }
}
