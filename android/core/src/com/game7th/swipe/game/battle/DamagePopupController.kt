package com.game7th.swipe.game.battle

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Align
import com.game7th.swipe.game.GameContextWrapper
import com.game7th.swipe.game.battle.model.BattleControllerEvent
import kotlin.math.max

class DamagePopupController(
        context: GameContextWrapper,
        id: Int,
        private val battle: BattleController,
        val originX: Float,
        val originY: Float,
        val value: Int,
        val color: Color
) : ElementController(context, id) {

    var timePassed = 0f
    val targetY = originY + 50f * battle.scale
    val targetWidth = 140f * battle.scale
    val showTime = 1.5f
    val disposeTime = 2f

    override fun render(batch: SpriteBatch, delta: Float) {
        timePassed += delta

        val progress = timePassed / showTime
        color.a = max(1f, progress)

        context.gameContext.font.apply {
            color = this@DamagePopupController.color
            data.scaleX = 1f + 1f * progress
            data.scaleY = 1f + 1f * progress
        }
        context.gameContext.font.draw(batch, value.toString(), originX - targetWidth / 2f, originY + (targetY - originY) * progress, targetWidth, Align.center, false)
        context.gameContext.font.apply {
            color = Color.WHITE
            data.scaleX = 1f
            data.scaleY = 1f
        }

        if (timePassed > disposeTime) {
            battle.removeController(this)
        }
    }

    override fun dispose() {
        super.dispose()
    }

    override fun handle(event: BattleControllerEvent) {}
}