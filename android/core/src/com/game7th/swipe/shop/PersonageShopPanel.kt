package com.game7th.swipe.shop

import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.game7th.metagame.dto.UnitType
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.util.ActionPanel
import com.game7th.swipe.util.InventoryAction

class PersonageShopPanel(
        private val context: GdxGameContext,
        val unitType: UnitType,
        val shopItemId: String,
        private val actions: List<InventoryAction>,
        private val equipper: (itemId: String, actionIndex: Int) -> Unit
): Group() {

    val bg = Image(context.battleAtlas.findRegion("vp_${unitType.getSkin()}")).apply {
        width = 220 * context.scale
        height = 310 * context.scale
    }

    val actionPanel = ActionPanel(context, context.scale * 200, actions, { index -> equipper(shopItemId, index)}).apply {
        x = 10f * context.scale
        y = 10f * context.scale
    }

    init {
        addActor(bg)
        addActor(actionPanel)

        width = bg.width
        height = bg.height
    }
}