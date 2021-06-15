package com.game7th.swipe.game.actors

import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.campaign.inventory.ItemView
import com.game7th.swipe.campaign.inventory.ItemViewAdapter
import com.game7th.swiped.api.FlaskItemFullInfoDto
import ktx.actors.onClick

class BoosterNodeView(
        private val context: GdxGameContext,
        private val size: Float,
        private val callback: (FlaskItemFullInfoDto) -> Unit
): Group() {

    val background = Image(context.uiAtlas.findRegion("inventory_bg")).apply {
        width = size
        height = size
    }
    var flaskStackView: ItemView? = null

    fun applyFlask(flask: FlaskItemFullInfoDto) {
        flaskStackView?.let { it.remove() }
        val itemViewSize = size * 0.76f
        flaskStackView = ItemView(context, ItemViewAdapter.PotionItemAdater(flask), false, 52f * context.scale).apply {
            x = (size - itemViewSize) / 2f
            y = (size - itemViewSize) / 2f
        }
        addActorAfter(background, flaskStackView)
        flaskStackView?.onClick { callback(flask) }
    }

    init {
        addActor(background)
    }
}