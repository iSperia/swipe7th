package com.game7th.swipe.game.battle

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.game7th.swipe.game.GameContextWrapper
import com.game7th.swipe.game.battle.model.BattleControllerEvent
import com.game7th.swipe.game.battle.model.EffectGdxModel

class AoeSteppedGeneratorOrchestrator(
        context: GameContextWrapper,
        id: Int,
        private val battle: BattleController,
        private val sourceFigure: FigureController,
        private val targetFigures: List<FigureController>,
        private val gdxEffect: EffectGdxModel
) : ElementController(context, id) {

    var timePassed = 0f
    var isStarted = false

    init {
        battle.lock(1)
    }

    override fun render(batch: SpriteBatch, delta: Float) {
        timePassed += delta

        if (!isStarted) {
            isStarted = true

            sourceFigure.switchPose(FigurePose.POSE_ATTACK)
        }
    }

    override fun dispose() {
    }

    override fun handle(event: BattleControllerEvent) {
        when (event) {
            is BattleControllerEvent.FigurePoseFrameIndexEvent -> {
                if (isStarted && event.figureId == sourceFigure.id) {
                    //we may launch the effect

                    val effect = SteppedGeneratorEffectController(
                            context,
                            battle.effectId++,
                            battle,
                            sourceFigure.x + 70f * battle.scale * (if (sourceFigure.flipped) -1 else 1),
                            sourceFigure.y,
                            if (sourceFigure.flipped) 0f else context.width,
                            targetFigures.map { it.x },
                            gdxEffect
                    )
                    battle.addEffect(effect)
                }
            }
            is BattleControllerEvent.SteppedGeneratorEvent -> {
                if (isStarted) {
                    val figure = targetFigures[event.index]
                    figure.switchPose(FigurePose.POSE_DAMAGE)

                    if (event.index == targetFigures.size - 1) {
                        battle.unlock()
                        battle.removeController(this)
                    }
                }
            }
        }
    }
}