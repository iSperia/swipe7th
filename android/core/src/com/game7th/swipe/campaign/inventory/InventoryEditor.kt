package com.game7th.swipe.campaign.inventory

import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.game7th.metagame.account.AccountService
import com.game7th.metagame.inventory.GearService
import com.game7th.swipe.ScreenContext
import kotlin.math.max

class InventoryEditor(
        private val context: ScreenContext,
        private val accountService: AccountService,
        private val gearService: GearService,
        private val personageId: Int
) : Group() {

    private val bg = Image(context.uiAtlas.findRegion("ui_dialog")).apply {
        width = context.scale * 480f
        height = context.scale * 200f
    }

    private val personageBgs = Group().apply {
        x = 10f * context.scale
        y = 10f * context.scale
    }

    private val textures = listOf("ui_item_bg_book", "ui_item_bg_boots", "ui_item_bg_ring", "ui_item_bg_body", "ui_item_bg_hand", "ui_item_bg_hat")

    val lx = 170f * context.scale
    val ly = 10f * context.scale

    val panelItems = Group()
    val panelScroller = ScrollPane(panelItems).apply {
        x = lx
        y = ly
        width = 300f * context.scale
        height = 180f * context.scale
    }

    init {
        addActor(bg)
        addActor(personageBgs)
        addActor(panelScroller)

        textures.forEachIndexed { index, texture ->
            val bg = Image(context.uiAtlas.findRegion(texture)).apply {
                x = if (index % 2 == 0) 0f else 60f * context.scale
                y = (index / 2) * 60f * context.scale
                width = 60f * context.scale
                height = 60f * context.scale
            }
            personageBgs.addActor(bg)
        }

        val items = gearService.listInventory()
        panelItems.width = context.scale * 60f * max((items.size - 1) / 3 + 1, 5)
        panelItems.height = 180f * context.scale

        val emptyItems = max(15 - items.size, items.size % 3)

        items.forEachIndexed { index, item ->
            val itemView = ItemView(context, item, true).apply {
                x = (index / 3) * 60f * context.scale
                y = (120f - (index % 3) * 60f) * context.scale
            }
            panelItems.addActor(itemView)
        }
        (1..emptyItems).forEach {
            val itemView = ItemView(context, null, true).apply {
                val index = items.size + it - 1
                x = (index / 3) * 60f * context.scale
                y = (120f - (index % 3) * 60f) * context.scale
            }
            panelItems.addActor(itemView)
        }
    }
}