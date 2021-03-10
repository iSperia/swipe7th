package com.game7th.swipe.game.battle

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.game7th.swipe.game.GameContextWrapper
import com.game7th.swipe.game.battle.model.BattleControllerEvent
import com.game7th.swipe.game.battle.model.EffectGdxModel

class BowStrikeOrchestrator(
        context: GameContextWrapper,
        id: Int,
        private val battle: BattleController,
        val sourceFigure: FigureController, //figure is launching projectiles
        val targets: List<FigureController>, //aims
        val effect: EffectGdxModel,
        val player: (String) -> Unit,
        val sound: String?
) : ElementController(context, id) {

    private var timePassed = 0f

    private var poseChanged = false
    private var triggeredLaunch = false
    private var effectId = mutableMapOf<Int, FigureController>()
    private var effectsDone = 0
    private var damageDone = 0

    override fun render(batch: SpriteBatch, delta: Float) {
        timePassed += delta * battle.timeScale()

        if (!poseChanged) {
            sound?.let { player(it) }
            sourceFigure.switchPose(FigurePose.POSE_ATTACK)
            poseChanged = true
        }
    }

    init {
        battle.lock(1)
    }

    override fun dispose() {
    }

    override fun handle(event: BattleControllerEvent) {
        when (event) {
            is BattleControllerEvent.FigurePoseFrameIndexEvent -> {
                if (!triggeredLaunch && event.figureId == sourceFigure.id) {
                    triggeredLaunch = true
                    targets.forEach {
                        effectId[battle.showEffectOverFigure(it, effect)] = it
                    }
                }
            }
            is BattleControllerEvent.EffectTriggerEvent -> {
                if (effectId.contains(event.effectId)) {
                    effectsDone++
                }
                if (effectsDone == targets.size) {
                    battle.unlock()
                    battle.removeController(this)
                }
            }
        }
    }
}