package com.game7th.swipe.game.battle

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.game7th.swipe.game.GameContextWrapper

class PersonageTickerController(
        context: GameContextWrapper,
        battle: BattleController,
        id: Int,
        val figure: FigureController
) : ElementController(context, battle, id) {

    var oldAbility = ""

    lateinit var texture: TextureRegion

    override fun render(batch: SpriteBatch, delta: Float) {
        figure.viewModel.stats.tickAbility?.let { tickAbility ->
            if (oldAbility != tickAbility) {
                texture = context.gameContext.battleAtlas.findRegion("pic_${tickAbility}")
                oldAbility = tickAbility
            }
            val ticksLeft = figure.viewModel.stats.maxTick - figure.viewModel.stats.tick
            batch.draw(texture, figure.x - context.scale * 15f, figure.y + (figure.figureModel.height * figure.figureModel.scale + 10f) * context.scale * battle.scale, context.scale * 30f, context.scale * 30f)
            context.gameContext.font.draw(batch, ticksLeft.toString(), figure.x - context.scale * 12f, figure.y + (figure.figureModel.height * figure.figureModel.scale + 23f) * battle.scale * context.scale)
        }
    }

    override fun dispose() {
        super.dispose()
    }
}