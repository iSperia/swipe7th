package com.game7th.swipe.campaign.inventory

import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.game7th.metagame.account.AccountService
import com.game7th.metagame.inventory.GearService
import com.game7th.metagame.inventory.InventoryItem
import com.game7th.metagame.inventory.ItemNode
import com.game7th.swipe.ScreenContext
import ktx.actors.onClick
import kotlin.math.max
import kotlin.math.min

class InventoryEditor(
        private val context: ScreenContext,
        private val accountService: AccountService,
        private val gearService: GearService,
        private val personageId: Int,
        private val refresher: () -> Unit
) : Group() {

    private val bg = Image(context.uiAtlas.findRegion("ui_dialog")).apply {
        width = context.scale * 480f
        height = context.scale * 200f
    }

    private val personageBgs = Group().apply {
        x = 10f * context.scale
        y = 10f * context.scale
    }

    private val equippedGroup = Group().apply {
        x = personageBgs.x
        y = personageBgs.y
    }

    private val textures = listOf("ui_item_bg_book", "ui_item_bg_boots", "ui_item_bg_ring", "ui_item_bg_body", "ui_item_bg_hand", "ui_item_bg_hat")
    private val xs = mapOf<ItemNode, Int>(ItemNode.BODY to 1, ItemNode.BOOK to 0, ItemNode.FOOT to 1, ItemNode.HAND to 0, ItemNode.RING to 0, ItemNode.HEAD to 1)
    private val ys = mapOf<ItemNode, Int>(ItemNode.BODY to 1, ItemNode.BOOK to 0, ItemNode.FOOT to 0, ItemNode.HAND to 2, ItemNode.RING to 1, ItemNode.HEAD to 2)

    val lx = 170f * context.scale
    val ly = 10f * context.scale

    val panelItems = Group()
    val panelScroller = ScrollPane(panelItems).apply {
        x = lx
        y = ly
        width = 300f * context.scale
        height = 180f * context.scale
    }

    var detailPanel: InventoryDetailPanel? = null

    init {
        addActor(bg)
        addActor(personageBgs)
        addActor(panelScroller)
        addActor(equippedGroup)

        textures.forEachIndexed { index, texture ->
            val bg = Image(context.uiAtlas.findRegion(texture)).apply {
                x = if (index % 2 == 0) 0f else 60f * context.scale
                y = (index / 2) * 60f * context.scale
                width = 60f * context.scale
                height = 60f * context.scale
            }
            personageBgs.addActor(bg)
        }

        reloadData()
    }

    private fun reloadData() {
        panelItems.children.forEach { it.remove() }
        equippedGroup.children.forEach { it.remove() }

        dismissDetailPanel()

        val items = gearService.listInventory()
        panelItems.width = context.scale * 60f * max((items.size - 1) / 3 + 1, 5)
        panelItems.height = 180f * context.scale

        val emptyItems = max(15 - items.size, 3 - items.size % 3)

        items.forEachIndexed { index, item ->
            val itemView = ItemView(context, item, true).apply {
                x = (index / 3) * 60f * context.scale
                y = (120f - (index % 3) * 60f) * context.scale
            }
            itemView.onClick {
                dismissDetailPanel()
                detailPanel = InventoryDetailPanel(
                        context,
                        item,
                        "Put on",
                        this@InventoryEditor::dismissDetailPanel,
                        this@InventoryEditor::equipFromDetailPanel).apply {
                    x = min(context.scale * 340f, panelScroller.x + itemView.x - panelScroller.scrollX)
                    y = panelScroller.y + itemView.y
                }
                this@InventoryEditor.addActor(detailPanel)
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

        accountService.getPersonages().firstOrNull { it.id == personageId }?.let { personage ->
            personage.items.forEach { item ->
                val itemView = ItemView(context, item, false)
                itemView.x = context.scale * 60f * xs[item.node]!!
                itemView.y = context.scale * 60f * ys[item.node]!!
                equippedGroup.addActor(itemView)
                itemView.onClick {
                    dismissDetailPanel()
                    detailPanel = InventoryDetailPanel(
                            context,
                            item,
                            "Wear off",
                            this@InventoryEditor::dismissDetailPanel,
                            this@InventoryEditor::dequipFromEquipped).apply {
                        x = min(context.scale * 340f, equippedGroup.x + itemView.x)
                        y = equippedGroup.y + itemView.y
                    }
                    this@InventoryEditor.addActor(detailPanel)
                }
            }
        }
    }

    private fun dismissDetailPanel() {
        detailPanel?.remove()
        detailPanel = null
    }

    private fun equipFromDetailPanel(item: InventoryItem) {
        accountService.equipItem(personageId, item)
        dismissDetailPanel()
        reloadData()
        refresher()
    }

    private fun dequipFromEquipped(item: InventoryItem) {
        accountService.dequipItem(personageId, item)
        dismissDetailPanel()
        reloadData()
        refresher()
    }
}