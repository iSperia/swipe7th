package com.game7th.swipe.game.actors

import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.game7th.swipe.game.BattleContext
import com.game7th.swiped.api.battle.TileViewModel

class StaticTileView(
        private val context: BattleContext,
        private val viewModel: TileViewModel,
        private val size: Float
) : AbstractTileView(), LayerProvider {

    val image: Image

    init {
        image = Image(context.battleAtlas.findRegion(viewModel.skin)).apply {
            width = size
            height = size
        }
        addActor(image)
    }

    override fun getLayer() = viewModel.layer

    override fun updateFrom(vm: TileViewModel) {
    }
}