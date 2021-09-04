package com.game7th.swipe.game.battle

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.game7th.swipe.game.BattleContext

class PersonageTickerController(
        context: BattleContext,
        battle: BattleController,
        id: Int,
        val figure: FigureController
) : ElementController(context, battle, id) {

    var oldAbility = ""

    lateinit var texture: TextureRegion

    override fun render(batch: SpriteBatch, delta: Float) {
        figure.viewModel.stats.tickAbility?.let { tickAbility ->
            if (oldAbility != tickAbility) {
                texture = context.battleAtlas.findRegion("pictogram_${tickAbility}")
                oldAbility = tickAbility
            }
            val ticksLeft = figure.viewModel.stats.maxTick - figure.viewModel.stats.tick
            val y = figure.y + figure.figureModel.height * figure.figureModel.scale * battle.scale + 30f * context.scale
            batch.draw(texture, figure.x - context.scale * 15f, y, context.scale * 30f, context.scale * 30f)
            context.gameContext.captionFont.draw(batch, ticksLeft.toString(), figure.x - context.scale * 12f, y + 10 * context.scale)
        }
    }

    override fun dispose() {
        super.dispose()
    }
}