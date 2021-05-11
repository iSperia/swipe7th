package com.game7th.swipe.game.battle

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.game7th.swipe.game.GameContextWrapper

class PersonageHealthbarController(
        context: GameContextWrapper,
        battle: BattleController,
        id: Int,
        val figure: FigureController
) : ElementController(context, battle, id) {

    private val healthBarBackground = TextureRegionDrawable(context.gameContext.battleAtlas.findRegion("hpbar_bg"))
    private val healthBarForeground = TextureRegionDrawable(context.gameContext.battleAtlas.findRegion("hpbar_scale"))
    private val healthBarBorder = NinePatchDrawable(context.gameContext.battleAtlas.createPatch("hpbar_border"))
    private val healthBarGlass = TextureRegionDrawable(context.gameContext.battleAtlas.findRegion("hpbar_fg"))
    private val resistForeground = TextureRegionDrawable(context.gameContext.battleAtlas.findRegion("hpbar_resist"))
    private val blackQuad = TextureRegionDrawable(context.gameContext.battleAtlas.findRegion("black_quad"))
    var timePassed: Float = 0f
    var timeHpActual: Float = timePassed
    var timeRsActual: Float = timePassed

    var lastKnownValue: Float = figure.viewModel.stats.health.toFloat()
    var displayedValue: Float = lastKnownValue
    var lastDisplayedValue: Float = displayedValue

    var lastKnownResist: Float = figure.viewModel.stats.resist.toFloat()
    var displayedResist: Float = lastKnownResist
    var lastDisplayedResist: Float = displayedResist

    override fun render(batch: SpriteBatch, delta: Float) {
        timePassed += delta * battle.timeScale()

        var value = figure.viewModel.stats.health.toFloat()
        if (value != lastKnownValue) {
            lastKnownValue = value
            timeHpActual = timePassed + 0.3f
            lastDisplayedValue = displayedValue
        } else if (timeHpActual <= timePassed) {
            displayedValue = value
        } else {
            displayedValue = value - (value - lastDisplayedValue) * (timeHpActual - timePassed) / 0.3f
        }

        val percent = displayedValue / figure.viewModel.stats.maxHealth

        val barWidth = 48f * battle.scale * context.scale
        val sx = figure.x - barWidth / 2
        val sy = figure.y - 15f * battle.scale

        val offset = barWidth * 0.04f
        val distance: Float = (barWidth - 2 * offset) * 100f / figure.viewModel.stats.maxHealth.toFloat()
        val isBigHealth = distance <= barWidth / 25
        val hei: Float = 20f * battle.scale
        val sectorHei: Float = (hei - 4f) / 2f

        healthBarBackground.draw(batch, sx, sy, barWidth, hei)
        healthBarForeground.region.let { r -> batch.draw(r.texture, sx, sy, barWidth * percent, hei, r.u, r.v2, r.u + (r.u2-r.u) * percent, r.v) }

        if (figure.viewModel.stats.resistMax > 0) {
            var resist = figure.viewModel.stats.resist.toFloat()
            if (resist != lastKnownResist) {
                lastKnownResist = resist
                timeRsActual = timePassed + 0.3f
                lastDisplayedResist = displayedResist
            } else if (timeRsActual <= timePassed) {
                displayedResist = resist
            } else {
                displayedResist = resist - (resist - lastDisplayedResist) * (timeRsActual - timePassed) / 0.3f
            }

            val resistPercent = displayedResist / figure.viewModel.stats.resistMax
            resistForeground.region.let { r -> batch.draw(r.texture, sx, sy, barWidth * resistPercent, hei, r.u, r.v2, r.u + (r.u2-r.u) * resistPercent, r.v) }
        }

        var cursor = distance
        var index = 1
        while (cursor < barWidth - 2 * offset) {
            val wid = if (index % 5 == 0) 2f * context.scale else 1f * context.scale
            val extraHei = if (index % 5 == 0) sectorHei else 0f
            if (index % 5 == 0 || !isBigHealth) {
                blackQuad.draw(batch, sx + offset + cursor, sy + 2f + sectorHei - extraHei, wid, sectorHei + extraHei)
            }
            index++
            cursor += distance
        }

        healthBarBorder.draw(batch, sx, sy, barWidth, hei)
        healthBarGlass.draw(batch, sx, sy, barWidth, hei)
    }

    override fun dispose() {
        super.dispose()
    }
}