package com.game7th.swipe.game.battle

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.game7th.battle.event.BattleEvent
import com.game7th.swipe.game.GameContextWrapper
import com.game7th.swipe.game.battle.model.BattleControllerEvent
import com.game7th.swipe.game.battle.model.GdxAttackType

/**
 * GDX graph controller for battle
 */
class BattleController(
        private val context: GameContextWrapper,
        private val y: Float,
        private val endEventHandler: (event: BattleEvent) -> Unit
) {

    /**
     * events to resolve
     */
    private val eventQueue = mutableListOf<BattleEvent>()

    /**
     * As soon as lock is zero, we are ready to process next event.
     * Use orchestrators to reduce event lock
     */
    private var eventProcessLock = 0

    private val figures = mutableListOf<FigureController>()
    private val effects = mutableListOf<ElementController>()

    private val backgroundTexture = context.gameContext.atlas.findRegion("battle_bg", 1)

    var effectId = 0
    val scale = 0.85f * context.scale

    val controllersToRemove = mutableListOf<ElementController>()

    fun enqueueEvent(event: BattleEvent) {
        eventQueue.add(event)
    }

    fun act(batch: SpriteBatch, delta: Float) {
        batch.draw(backgroundTexture, 0f, y, context.width, context.width * 0.67f)

        figures.forEach {
            it.render(batch, delta)
        }
        effects.forEach {
            it.render(batch, delta)
        }

        if (eventProcessLock == 0 && eventQueue.isNotEmpty()) {
            val event = eventQueue.removeAt(0)
            //ok, we have an event
            processEvent(event)
        }

        effects.removeAll(controllersToRemove)
        controllersToRemove.clear()
    }

    private val paddingSide = context.width * 0.05f

    private fun processEvent(event: BattleEvent) {
        when (event) {
            is BattleEvent.CreatePersonageEvent -> {
                val x = paddingSide + (context.width - 2 * paddingSide) * 0.2f * (0.5f + event.position)
                val figure = FigureController(context,
                        event.personage.id,
                        this@BattleController,
                        context.gdxModel.figure(event.personage.skin) ?: context.gdxModel.figure("personage_slime")!!,
                        x,
                        y,
                        scale,
                        event.personage.team > 0)
                figures.add(figure)
            }
            is BattleEvent.PersonageAttackEvent -> {
                handleAttack(event)
            }
            is BattleEvent.PersonageDamageEvent -> {
                val figure = figures.first { it.id == event.personage.id }
                val controller = DamagePopupController(
                        context,
                        effectId++,
                        this,
                        figure.originX,
                        figure.originY + figure.figureModel.height * scale + 10 * scale,
                        event.damage,
                        Color.RED
                )
                effects.add(controller)
            }
            is BattleEvent.PersonageDeadEvent -> {
                val figure = figures.first { it.id == event.personage.id }
                figure.switchPose(FigurePose.POSE_DEATH)
                figure.isDead = true
            }
            is BattleEvent.PersonageHealEvent -> {
                val figure = figures.first { it.id == event.personage.id }
                val controller = DamagePopupController(
                        context,
                        effectId++,
                        this,
                        figure.originX,
                        figure.originY + figure.figureModel.height * scale + 10 * scale,
                        event.amount,
                        Color.GREEN
                )
                effects.add(controller)
            }
            is BattleEvent.PersonageUpdateEvent -> {
                val figure = figures.first { it.id == event.personage.id }
            }
            is BattleEvent.VictoryEvent -> {
                endEventHandler(event)
            }
            is BattleEvent.DefeatEvent -> {
                endEventHandler(event)
            }
        }
    }

    private fun handleAttack(event: BattleEvent.PersonageAttackEvent) {
        val figure = figures.firstOrNull { it.id == event.source.id }
        figure?.let { figure ->
            val attack = figure.figureModel.attacks[event.attackIndex]
            when (attack.attackType) {
                GdxAttackType.MOVE_AND_PUNCH -> {
                    val targetPersonage = event.targets.firstOrNull()
                    targetPersonage?.let { targetPersonage ->
                        val targetFigure = figures.firstOrNull { it.id == targetPersonage.first.id }
                        targetFigure?.let { targetFigure ->
                            val orchestrator = MovePunchOrchestrator(
                                    context,
                                    effectId++,
                                    this,
                                    figure,
                                    targetFigure
                            )
                            effects.add(orchestrator)
                        }
                    }
                    if (targetPersonage == null) {
                        val orchestrator = MovePunchOrchestrator(
                                context,
                                effectId++,
                                this,
                                figure,
                                figure
                        )
                        effects.add(orchestrator)
                    }
                }
                GdxAttackType.AOE_STEPPED_GENERATOR -> {
                    figure.figureModel.attacks[event.attackIndex].effect?.let { effect ->
                        val orchestrator = AoeSteppedGeneratorOrchestrator(
                                context,
                                effectId++,
                                this@BattleController,
                                figure,
                                event.targets.map { target -> figures.first { it.id == target.first.id } }.sortedBy { it.x },
                                effect)
                        effects.add(orchestrator)
                    }
                }
            }
            Unit
        }
    }

    fun lock(lock: Int) {
        eventProcessLock += lock
    }

    fun unlock() {
        eventProcessLock--
    }

    fun removeController(controller: ElementController) {
        controller.dispose()
        controllersToRemove.add(controller)
    }

    fun propagate(event: BattleControllerEvent) {
        effects.forEach {
            it.handle(event)
        }
    }

    fun addEffect(effect: SteppedGeneratorEffectController) {
        effects.add(effect)
    }
}
