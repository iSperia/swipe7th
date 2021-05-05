package com.game7th.swipe.game.actors

import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.campaign.inventory.ItemView
import com.game7th.swipe.campaign.inventory.ItemViewAdapter
import com.game7th.swiped.api.FlaskItemFullInfoDto

class BoosterNodeView(
        private val context: GdxGameContext,
        private val size: Float
): Group() {

    val background = Image(context.battleAtlas.findRegion("flask_bg")).apply {
        width = size
        height = size
    }
    var flaskStackView: ItemView? = null
    val foreground = Image(context.battleAtlas.findRegion("flask_fg")).apply {
        width = size
        height = size
    }

    fun applyFlask(flask: FlaskItemFullInfoDto) {
        flaskStackView?.let { it.remove() }
        val itemViewSize = size * 0.76f
        flaskStackView = ItemView(context, ItemViewAdapter.PotionItemAdater(flask), false, size * 0.8f).apply {
            x = (size - itemViewSize) / 2f
            y = (size - itemViewSize) / 2f
        }
        addActorAfter(background, flaskStackView)
    }

    init {
        addActor(background)
        addActor(foreground)
    }
}