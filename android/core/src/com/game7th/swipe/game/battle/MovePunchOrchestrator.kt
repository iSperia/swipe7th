package com.game7th.swipe.game.battle

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.game7th.swipe.game.GameContextWrapper
import com.game7th.swipe.game.battle.model.BattleControllerEvent

class MovePunchOrchestrator(
        context: GameContextWrapper,
        id: Int,
        private val battle: BattleController,
        private val sourceFigure: FigureController,
        private val targetFigure: FigureController
) : ElementController(context, id) {

    var timePassed = 0f

    var timeStampMove = 0.3f
    var isPunchStarted = false
    var timeStampBackMove = 0.3f
    var timeStampBackMoveStart = 0f

    var direction = if (sourceFigure.flipped) -1 else 1

    var targetX = targetFigure.x - direction * battle.scale * 70f

    init {
        battle.lock(1)
        if (sourceFigure == targetFigure) {
            timePassed = timeStampMove
        }
    }

    override fun render(batch: SpriteBatch, delta: Float) {
        timePassed += delta

        if (timePassed < timeStampMove && sourceFigure != targetFigure) {
            sourceFigure.x = sourceFigure.originX + (timePassed / timeStampMove) * (targetX - sourceFigure.originX)
        } else if (!isPunchStarted) {
            sourceFigure.x = if (sourceFigure != targetFigure) targetX else sourceFigure.originX
            sourceFigure.switchPose(FigurePose.POSE_ATTACK)
            isPunchStarted = true
        } else if (sourceFigure != targetFigure) {
            if (timeStampBackMoveStart + timeStampMove > timePassed) {
                sourceFigure.x = targetX + ((timePassed - timeStampBackMoveStart) / timeStampBackMove) * (sourceFigure.originX - targetX)
            } else if (timeStampBackMoveStart > 0f && timeStampBackMoveStart + timeStampMove <= timePassed) {
                sourceFigure.x = sourceFigure.originX
                battle.removeController(this)
                battle.unlock()
            }
        }
    }

    override fun dispose() {
        timeStampBackMoveStart = 0f
    }

    override fun handle(event: BattleControllerEvent) {
        when (event) {
            is BattleControllerEvent.FigurePoseFrameIndexEvent -> {
                if (event.figureId == sourceFigure.id && timePassed > timeStampMove) {
                    if (sourceFigure != targetFigure) {
                        targetFigure.switchPose(FigurePose.POSE_DAMAGE)

                        timeStampBackMoveStart = timePassed
                    } else {
                        battle.removeController(this)
                        battle.unlock()
                    }
                }
            }
        }
    }
}