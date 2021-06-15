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

    private val lifebarHealth = TextureRegionDrawable(context.gameContext.battleAtlas.findRegion("lifebar_health"))
    private val lifebarBackgroundBottom = TextureRegionDrawable(context.gameContext.battleAtlas.findRegion("lifebar_background_bottom"))
    private val lifebarBackgroundTop = TextureRegionDrawable(context.gameContext.battleAtlas.findRegion("lifebar_background_top"))
    private val lifebarBorderBottom = NinePatchDrawable(context.gameContext.battleAtlas.createPatch("lifebar_border_bottom"))
    private val lifebarBorderTop = NinePatchDrawable(context.gameContext.battleAtlas.createPatch("lifebar_border_top"))
    private val lifebarLine = TextureRegionDrawable(context.gameContext.battleAtlas.findRegion("lifebar_line"))
    private val lifebarResist = TextureRegionDrawable(context.gameContext.battleAtlas.findRegion("lifebar_resist"))

    var timePassed: Float = 0f
    var timeHpActual: Float = timePassed
    var timeRsActual: Float = timePassed

    var lastKnownValue: Float = figure.viewModel.stats.health.toFloat()
    var displayedValue: Float = lastKnownValue
    var lastDisplayedValue: Float = displayedValue

    var lastKnownResist: Float = figure.viewModel.stats.resist.toFloat()
    var displayedResist: Float = lastKnownResist
    var lastDisplayedResist: Float = displayedResist

    private var bottomHei = 4f * context.scale
    private var topHei = 8f * context.scale

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

        val barWidth = 200f * battle.scale * 0.8f
        val sx = figure.x - barWidth / 2f
        val sy = figure.y + figure.figureModel.height * figure.figureModel.scale * battle.scale + 8f * context.scale

        val distance: Float = barWidth * 100f / figure.viewModel.stats.maxHealth.toFloat()
        val isBigHealth = distance <= barWidth / 25
        val hei: Float = 20f * battle.scale
        val sectorHei: Float = topHei / 2f

        lifebarBackgroundTop.draw(batch, sx, sy + bottomHei, barWidth, topHei)
        lifebarHealth.region.let { r -> batch.draw(r.texture, sx, sy + bottomHei, barWidth * percent, topHei, r.u, r.v2, r.u + (r.u2-r.u) * percent, r.v) }

        if (figure.viewModel.stats.resistMax > 0) {
            lifebarBackgroundBottom.draw(batch, sx, sy, barWidth, 3f * context.scale)
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
            lifebarResist.region.let { r -> batch.draw(r.texture, sx, sy, barWidth * resistPercent, 3f * context.scale, r.u, r.v2, r.u + (r.u2-r.u) * resistPercent, r.v) }
            lifebarBorderBottom.draw(batch, sx, sy, barWidth, bottomHei)
        }

        var cursor = distance
        var index = 1
        while (cursor < barWidth) {
            val extraHei = if (index % 5 == 0) sectorHei else 0f
            if (index % 5 == 0 || !isBigHealth) {
                lifebarLine.draw(batch, sx + cursor, sy + bottomHei + sectorHei - extraHei, context.scale, sectorHei + extraHei)
            }
            index++
            cursor += distance
        }

        lifebarBorderTop.draw(batch, sx, sy + bottomHei, barWidth, topHei)
    }

    override fun dispose() {
        super.dispose()
    }
}