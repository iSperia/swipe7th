package com.game7th.swipe.game.battle

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.game7th.swipe.game.BattleContext

class PersonageHealthbarController(
        context: BattleContext,
        battle: BattleController,
        id: Int,
        val figure: FigureController
) : ElementController(context, battle, id) {

    private val lifebarStripe = TextureRegionDrawable(context.battleAtlas.findRegion("lifebar_stripe"))
    private val lifebarBackground = TextureRegionDrawable(context.battleAtlas.findRegion("lifebar_bg"))
    private val lifebarVertBig = TextureRegionDrawable(context.battleAtlas.findRegion("lifebar_vert_big"))
    private val lifebarVertSmall = TextureRegionDrawable(context.battleAtlas.findRegion("lifebar_vert_small"))
    private val resbarBackground = TextureRegionDrawable(context.battleAtlas.findRegion("resbar_bg"))
    private val resbarStripe = TextureRegionDrawable(context.battleAtlas.findRegion("resbar_stripe"))
    private val resbarVertBig = TextureRegionDrawable(context.battleAtlas.findRegion("resbar_vert_big"))
    private val resbarVertSmall = TextureRegionDrawable(context.battleAtlas.findRegion("resbar_vert_small"))

    var timePassed: Float = 0f
    var timeHpActual: Float = timePassed
    var timeRsActual: Float = timePassed

    var lastKnownValue: Float = figure.viewModel.stats.health.toFloat()
    var displayedValue: Float = lastKnownValue
    var lastDisplayedValue: Float = displayedValue

    var lastKnownResist: Float = figure.viewModel.stats.resist.toFloat()
    var displayedResist: Float = lastKnownResist
    var lastDisplayedResist: Float = displayedResist

    private var bottomHei = 2f * context.scale
    private var lifebarBigHei = 4f * context.scale
    private var lifebarSmallHei = 2f * context.scale
    private var lifebarVertWidth = 1f * context.scale
    private var resbarHei = 2f * context.scale
    private var resbarVertWidth = 1f * context.scale
    private var lifebarOffset = 3f * context.scale
    private var lifebarTotalHei = 10f * context.scale
    private var resbarTotalHei = 6f * context.scale
    private val resbarOffset = 2f * context.scale

    override fun render(batch: SpriteBatch, delta: Float) {
        timePassed += delta

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

        val barWidth = 79f * context.scale
        val sx = figure.x - barWidth / 2f
        val sy = figure.y + figure.figureModel.height * figure.figureModel.scale * battle.scale + resbarTotalHei

        val distance: Float = barWidth * 100f / figure.viewModel.stats.maxHealth.toFloat()
        val isBigHealth = distance <= barWidth / 25

        lifebarBackground.draw(batch, sx, sy, barWidth, lifebarTotalHei)
        lifebarStripe.region.let { r -> batch.draw(r.texture, sx + lifebarOffset, sy + lifebarOffset, (barWidth - 2 * lifebarOffset) * percent, lifebarBigHei, r.u, r.v2, r.u + (r.u2-r.u) * percent, r.v) }

        if (figure.viewModel.stats.resistMax > 0) {
            resbarBackground.draw(batch, sx, sy - resbarTotalHei, barWidth, resbarTotalHei)
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
            resbarStripe.region.let { r -> batch.draw(r.texture, sx + resbarOffset, sy - resbarTotalHei + resbarOffset, (barWidth - 2 * resbarOffset) * resistPercent, resbarHei, r.u, r.v2, r.u + (r.u2-r.u) * resistPercent, r.v) }
        }
//
//        var cursor = distance
//        var index = 1
//        while (cursor < barWidth) {
//            val extraHei = if (index % 5 == 0) sectorHei else 0f
//            if (index % 5 == 0 || !isBigHealth) {
//                lifebarVertBig.draw(batch, sx + cursor, sy + bottomHei + sectorHei - extraHei, context.scale, sectorHei + extraHei)
//            }
//            index++
//            cursor += distance
//        }
//
//        lifebarBorderTop.draw(batch, sx, sy + bottomHei, barWidth, topHei)
    }

    override fun dispose() {
        super.dispose()
    }
}