package com.game7th.swipe.game.battle

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Align
import com.game7th.swipe.game.BattleContext
import kotlin.math.max

class DamagePopupController(
        context: BattleContext,
        battle: BattleController,
        id: Int,
        val originX: Float,
        val originY: Float,
        val value: Int,
        val color: Color
) : ElementController(context, battle, id) {

    var timePassed = 0f
    val targetY = originY + 50f * battle.scale
    val targetWidth = 140f * battle.scale
    val showTime = 1.5f
    val disposeTime = 2f

    override fun render(batch: SpriteBatch, delta: Float) {
        timePassed += delta * battle.timeScale()

        val progress = timePassed / showTime
        color.a = max(1f, progress)

        context.gameContext.regularFont.apply {
            color = this@DamagePopupController.color
            data.scaleX = 1f + 1f * progress
            data.scaleY = 1f + 1f * progress
        }
        context.gameContext.regularFont.draw(batch, value.toString(), originX - targetWidth / 2f, originY + (targetY - originY) * progress, targetWidth, Align.center, false)
        context.gameContext.regularFont.apply {
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
}