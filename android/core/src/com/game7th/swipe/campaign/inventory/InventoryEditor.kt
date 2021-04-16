package com.game7th.swipe.campaign.inventory

import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.game7th.metagame.account.AccountService
import com.game7th.metagame.inventory.GearService
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.util.InventoryAction
import com.game7th.swiped.api.InventoryItemFullInfoDto
import com.game7th.swiped.api.ItemNode
import kotlinx.coroutines.launch
import ktx.actors.onClick
import ktx.async.KtxAsync
import kotlin.math.max
import kotlin.math.min

class InventoryEditor(
        private val context: GdxGameContext,
        private val accountService: AccountService,
        private val gearService: GearService,
        private val personageId: String,
        private val refresher: () -> Unit
) : Group() {

    private val bg = Image(context.uiAtlas.createPatch("ui_hor_panel")).apply {
        width = context.scale * 480f
        height = context.scale * 200f
    }

    private val personageBgs = Group().apply {
        x = 10f * context.scale
        y = 10f * context.scale
    }

    val equippedGroup = Group().apply {
        x = personageBgs.x
        y = personageBgs.y
        width = 120f * context.scale
        height = 180f * context.scale
    }

    private val textures = listOf("ui_item_bg_book", "ui_item_bg_boots", "ui_item_bg_ring", "ui_item_bg_body", "ui_item_bg_hand", "ui_item_bg_hat")
    private val xs = mapOf(ItemNode.BODY to 1, ItemNode.BOOK to 0, ItemNode.FOOT to 1, ItemNode.HAND to 0, ItemNode.RING to 0, ItemNode.HEAD to 1)
    private val ys = mapOf(ItemNode.BODY to 1, ItemNode.BOOK to 0, ItemNode.FOOT to 0, ItemNode.HAND to 2, ItemNode.RING to 1, ItemNode.HEAD to 2)

    val lx = 170f * context.scale
    val ly = 10f * context.scale

    val panelItems = Group()
    val panelScroller = ScrollPane(panelItems).apply {
        x = lx
        y = ly
        width = 300f * context.scale
        height = 180f * context.scale
    }

    var detailPanel: ItemDetailPanel? = null

    var dirty = false

    init {
        addActor(bg)
        addActor(personageBgs)
        addActor(panelScroller)
        addActor(equippedGroup)

        bg.onClick { dismissDetailPanel() }

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
        KtxAsync.launch {
            val items = gearService.listInventory().filter { it.personageId == null }
            panelItems.width = context.scale * 60f * max((items.size - 1) / 3 + 1, 5)
            panelItems.height = 180f * context.scale
            panelScroller.actor = panelItems

            panelItems.children.forEach { it.clearActions() }
            equippedGroup.children.forEach { it.clearActions() }
            panelItems.clearChildren()
            equippedGroup.clearChildren()

            dismissDetailPanel()

            val emptyItems = max(15 - items.size, 3 - items.size % 3)

            items.forEachIndexed { index, item ->
                val itemView = ItemView(context, ItemViewAdapter.InventoryItemAdapter(item), true).apply {
                    x = (index / 3) * 60f * context.scale
                    y = (120f - (index % 3) * 60f) * context.scale
                }
                itemView.onClick {
                    processInventoryItemClick(item, itemView)
                }
                panelItems.addActor(itemView)
            }
            (1..emptyItems).forEach {
                val itemView = ItemView(context, ItemViewAdapter.EmptyAdapter, true).apply {
                    val index = items.size + it - 1
                    x = (index / 3) * 60f * context.scale
                    y = (120f - (index % 3) * 60f) * context.scale
                }
                panelItems.addActor(itemView)
            }

            accountService.getPersonages().firstOrNull { it.id == personageId }?.let { personage ->
                gearService.listInventory().filter { it.personageId == personage.id }.forEach { item ->
                    val itemView = ItemView(context, ItemViewAdapter.InventoryItemAdapter(item), false)
                    itemView.x = context.scale * 60f * xs[item.template.node]!!
                    itemView.y = context.scale * 60f * ys[item.template.node]!!
                    equippedGroup.addActor(itemView)
                    itemView.onClick {
                        dismissDetailPanel()
                        detailPanel = ItemDetailPanel(
                                context,
                                ItemViewAdapter.InventoryItemAdapter(item),
                                listOf(com.game7th.swipe.util.InventoryAction.StringAction("Wear off")),
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
    }

    fun processInventoryItemClick(item: InventoryItemFullInfoDto, itemView: ItemView) {
        dismissDetailPanel()
        detailPanel = ItemDetailPanel(
                context,
                ItemViewAdapter.InventoryItemAdapter(item),
                listOf(InventoryAction.StringAction("Put on")),
                this@InventoryEditor::dismissDetailPanel,
                this@InventoryEditor::equipFromDetailPanel).apply {
            x = min(context.scale * 340f, panelScroller.x + itemView.x - panelScroller.scrollX)
            y = panelScroller.y + itemView.y
        }
        this@InventoryEditor.addActor(detailPanel)
    }

    private fun dismissDetailPanel() {
        detailPanel?.remove()
        detailPanel = null
    }

    fun equipFromDetailPanel(actionIndex: Int, meta: String?) {
        (detailPanel?.item as? ItemViewAdapter.InventoryItemAdapter)?.item?.let { item ->
            KtxAsync.launch {
                gearService.equipItem(personageId, item)
                dismissDetailPanel()
                dirty = true
                refresher()
            }
        }
    }

    private fun dequipFromEquipped(actionIndex: Int, meta: String?) {
        (detailPanel?.item as? ItemViewAdapter.InventoryItemAdapter)?.item?.let { item ->
            KtxAsync.launch {
                gearService.dequipItem(personageId, item)
                dismissDetailPanel()
                dirty = true
                refresher()
            }
        }
    }

    override fun act(delta: Float) {
        super.act(delta)
        if (dirty) {
            dirty = false
            reloadData()
        }
    }
}