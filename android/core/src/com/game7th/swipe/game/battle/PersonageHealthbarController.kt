package com.game7th.swipe.game.battle

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.game7th.swipe.game.GameContextWrapper

class PersonageHealthbarController(
        context: GameContextWrapper,
        battle: BattleController,
        id: Int,
        val figure: FigureController
) : ElementController(context, battle, id) {

    private val healthBarBackground = context.gameContext.battleAtlas.createPatch("hp_bar_bg")
    private val healthBarForeground = TextureRegionDrawable(context.gameContext.battleAtlas.findRegion("hp_bar_rect"))
    private val resistForeground = TextureRegionDrawable(context.gameContext.battleAtlas.findRegion("rs_bar_rect"))
    private val blackQuad = TextureRegionDrawable(context.gameContext.battleAtlas.findRegion("black_quad"))
    var timePassed: Float = 0f
    var timeHpActual: Float = timePassed
    val healthBarWidth = 96f * battle.scale - 12f

    var lastKnownValue: Float = figure.viewModel.stats.health.toFloat()
    var displayedValue: Float = lastKnownValue
    var lastDisplayedValue: Float = displayedValue

    private var distance: Float = (healthBarWidth - 4f) * 100f / figure.viewModel.stats.maxHealth.toFloat()
    private var isBigHealth = distance <= healthBarWidth / 25
    private val hei: Float = 16f * battle.scale
    private val sectorHei: Float = (hei - 4f) / 2f

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

        val sx = figure.x - 48f * battle.scale
        val sy = figure.y - 15f * battle.scale

        healthBarBackground.draw(batch, sx, sy, 96f * battle.scale, hei)
        healthBarForeground.draw(batch, sx + 6f, sy + 2f, healthBarWidth * percent, hei - 4f)

        if (figure.viewModel.stats.resistMax > 0) {
            val resistPercent = figure.viewModel.stats.resist.toFloat() / figure.viewModel.stats.resistMax
            resistForeground.draw(batch, sx + 6f, sy + 2f, healthBarWidth * resistPercent, hei - 4f)
        }

        var cursor = distance
        var index = 1
        while (cursor < healthBarWidth) {
            val wid = if (index % 5 == 0) 2f * context.scale else 1f * context.scale
            val extraHei = if (index % 5 == 0) sectorHei else 0f
            if (index % 5 == 0 || !isBigHealth) {
                blackQuad.draw(batch, sx + 2f + cursor, sy + 2f + sectorHei - extraHei, wid, sectorHei + extraHei)
            }
            index++
            cursor += distance
        }
    }

    override fun dispose() {
        super.dispose()
    }
}