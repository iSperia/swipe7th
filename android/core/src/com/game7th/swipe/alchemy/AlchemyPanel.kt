package com.game7th.swipe.alchemy

import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.game7th.metagame.inventory.GearService
import com.game7th.metagame.inventory.dto.FlaskStackDto
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.campaign.inventory.ItemDetailPanel
import com.game7th.swipe.campaign.inventory.ItemView
import com.game7th.swipe.campaign.inventory.ItemViewAdapter
import com.game7th.swipe.forge.ForgePanel
import com.game7th.swipe.util.InventoryAction
import ktx.actors.onClick
import kotlin.math.max
import kotlin.math.min

sealed class AlchemyPanelMode {
    object CraftMode: AlchemyPanelMode()
    class DrinkMode(
            val useFlaskCallback: ((FlaskStackDto) -> Unit)
    ): AlchemyPanelMode()
}

class AlchemyPanel(
        private val context: GdxGameContext,
        private val gearService: GearService,
        private val mode: AlchemyPanelMode
): Group() {

    val panelItems = Group()
    val panelScroller = ScrollPane(panelItems).apply {
        x = 40f * context.scale
        y = 20f * context.scale
        width = 400f * context.scale
        height = 240f * context.scale
    }

    val bg = Image(context.uiAtlas.findRegion("ui_bg_alch")).apply {
        width = context.scale * 480f
        height = context.scale * ForgePanel.h
    }

    var detailPanel: ItemDetailPanel? = null

    init {
        addActor(bg)
        addActor(panelScroller)
        reloadData()
    }

    fun reloadData() {
        val flasks = gearService.listFlasks()
        panelItems.width = context.scale * 80 * max((flasks.size - 1) / 3 + 1, 5)
        panelItems.height = 240 * context.scale
        panelScroller.actor = panelItems

        panelItems.children.forEach { it.remove() }

        val emptyItems = max(15 - flasks.size, 3 - flasks.size % 3)

        flasks.forEachIndexed { index, flask ->
            val itemView = ItemView(context, ItemViewAdapter.PotionItemAdater(flask), true, 80f * context.scale).apply {
                x = (index / 3) * 80f * context.scale
                y = (160f - (index % 3) * 80f) * context.scale
            }
            itemView.onClick {
                dismissDetailPanel()
                detailPanel = ItemDetailPanel(
                        context,
                        ItemViewAdapter.PotionItemAdater(flask),
                        produceActions(),
                        this@AlchemyPanel::dismissDetailPanel,
                        this@AlchemyPanel::processAction).apply {
                    x = min(context.scale * 320f, panelScroller.x + itemView.x)
                    y = panelScroller.y + itemView.y
                }
                this@AlchemyPanel.addActor(detailPanel)
            }
            panelItems.addActor(itemView)
        }
        (1..emptyItems).forEach {
            val itemView = ItemView(context, ItemViewAdapter.EmptyAdapter, true, 80f * context.scale).apply {
                val index = flasks.size + it - 1
                x = (index / 3) * 80f * context.scale
                y = (160f - (index % 3) * 80f) * context.scale
            }
            panelItems.addActor(itemView)
        }
    }

    private fun produceActions() = when (mode) {
        is AlchemyPanelMode.CraftMode -> emptyList<InventoryAction>()
        is AlchemyPanelMode.DrinkMode -> listOf(
                InventoryAction.StringAction("Drink")
        )
    }

    private fun dismissDetailPanel() {
        detailPanel?.remove()
        detailPanel = null
    }

    fun processAction(index: Int, meta: String?) {
        when (mode) {
            is AlchemyPanelMode.DrinkMode -> {
                (detailPanel?.item as? ItemViewAdapter.PotionItemAdater)?.potion?.let { potion ->
                    mode.useFlaskCallback(potion)
                }
            }
        }
    }

    companion object {
        const val h = 300f
    }
}