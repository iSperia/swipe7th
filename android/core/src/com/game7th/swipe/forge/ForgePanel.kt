package com.game7th.swipe.forge

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.utils.Align
import com.game7th.metagame.account.AccountService
import com.game7th.metagame.account.dto.Currency
import com.game7th.metagame.inventory.GearService
import com.game7th.metagame.inventory.dto.InventoryItem
import com.game7th.swipe.BaseScreen
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.TutorialKeys
import com.game7th.swipe.campaign.inventory.ItemDetailPanel
import com.game7th.swipe.campaign.inventory.ItemView
import com.game7th.swipe.campaign.inventory.ItemViewAdapter
import com.game7th.swipe.dialog.DismissStrategy
import com.game7th.swipe.util.InventoryAction
import com.game7th.swipe.util.bounds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ktx.actors.onClick
import ktx.async.KtxAsync
import kotlin.math.max
import kotlin.math.min

class ForgePanel(
        private val context: GdxGameContext,
        private val screen: BaseScreen,
        private val gearService: GearService,
        private val accountService: AccountService
) : Group() {

    val dustIcon = Image(context.uiAtlas.findRegion("ui_currency_dust")).apply {
        x = 40f * context.scale
        y = (h - 10f - 24f) * context.scale
        width = 24f * context.scale
        height = 24f * context.scale
    }

    val dustLabel = Label((accountService.getBalance().currencies[Currency.DUST] ?: 0).toString(), Label.LabelStyle(context.font, Color.WHITE)).apply {
        x = 69f * context.scale
        y = dustIcon.y
        width = 72f * context.scale
        height = 24f * context.scale
        setAlignment(Align.left)
        setFontScale(24f * context.scale / 36f)
    }

    val panelItems = Group()
    val panelScroller = ScrollPane(panelItems).apply {
        x = 40f * context.scale
        y = 20f * context.scale
        width = 400f * context.scale
        height = 240f * context.scale
    }

    var detailPanel: ItemDetailPanel? = null

    val bg = Image(context.uiAtlas.findRegion("ui_bg_forge")).apply {
        width = context.scale * 480f
        height = context.scale * h
    }

    init {
        addActor(bg)
        addActor(panelScroller)
        addActor(dustIcon)
        addActor(dustLabel)
        reloadData()

        val items = gearService.listInventory()
        if (items.isNotEmpty() && context.storage.get(TutorialKeys.TUTORIAL_FORGE)?.toBoolean() != true) {
            KtxAsync.launch {
                delay(300)
                screen.showFocusView("ttr_forge_1", bg.bounds(), DismissStrategy.DISMISS_ON_OUTSIDE) {
                    screen.showFocusView("ttr_forge_2", dustLabel.bounds(), DismissStrategy.DISMISS_ON_OUTSIDE) {
                        screen.showFocusView("ttr_forge_3", panelItems.getChild(0).bounds(), DismissStrategy.DISMISS_ON_INSIDE) {
                            processItemClicked(items[0], panelItems.getChild(0) as ItemView)
                            KtxAsync.launch {
                                delay(50)
                                detailPanel?.let { detail ->
                                    screen.showFocusView("ttr_forge_4", detail.actionGroup.getChild(4).bounds(), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                        screen.showFocusView("ttr_forge_5", detail.actionGroup.getChild(0).bounds(), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                            context.storage.put(TutorialKeys.TUTORIAL_FORGE, "true")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun dismissDetailPanel() {
        detailPanel?.remove()
        detailPanel = null
    }

    private fun reloadData() {
        val items = gearService.listInventory()
        panelItems.width = context.scale * 80 * max((items.size - 1) / 3 + 1, 5)
        panelItems.height = 240 * context.scale
        panelScroller.actor = panelItems

        dustLabel.setText((accountService.getBalance().currencies[Currency.DUST] ?: 0).toString())

        panelItems.children.forEach { it.remove() }

        dismissDetailPanel()

        val emptyItems = max(15 - items.size, 3 - items.size % 3)

        items.forEachIndexed { index, item ->
            val itemView = ItemView(context, ItemViewAdapter.InventoryItemAdapter(item), true, 80f * context.scale).apply {
                x = (index / 3) * 80f * context.scale
                y = (160f - (index % 3) * 80f) * context.scale
            }
            itemView.onClick {
                processItemClicked(item, itemView)
            }
            panelItems.addActor(itemView)
        }
        (1..emptyItems).forEach {
            val itemView = ItemView(context, ItemViewAdapter.EmptyAdapter, true, 80f * context.scale).apply {
                val index = items.size + it - 1
                x = (index / 3) * 80f * context.scale
                y = (160f - (index % 3) * 80f) * context.scale
            }
            panelItems.addActor(itemView)
        }
    }

    private fun processItemClicked(item: InventoryItem, itemView: ItemView) {
        dismissDetailPanel()
        detailPanel = ItemDetailPanel(
                context,
                ItemViewAdapter.InventoryItemAdapter(item),
                listOf(InventoryAction.IconAction("+${item.level * 100}", "ui_button_dust", Currency.DUST), InventoryAction.IconAction("-${item.level * 100}", "ui_button_levelup", Currency.DUST)),
                this@ForgePanel::dismissDetailPanel,
                this@ForgePanel::processAction).apply {
            x = min(context.scale * 320f, panelScroller.x + itemView.x)
            y = panelScroller.y + itemView.y
        }
        this@ForgePanel.addActor(detailPanel)
    }

    fun processAction(index: Int, meta: String?) {
        (detailPanel?.item as? ItemViewAdapter.InventoryItemAdapter)?.item?.let { item ->
            when (index) {
                0 -> { //to dust!
                    accountService.fund(Currency.DUST, item.level * 100)
                    gearService.removeItem(item)
                }
                1 -> { //level up
                    val dustNeeded = 100 * item.level
                    if ((accountService.getBalance().currencies[Currency.DUST] ?: 0) >= dustNeeded) {
                        accountService.spend(Currency.DUST, dustNeeded)
                        gearService.upgradeItem(item)
                    }
                }
            }
            dismissDetailPanel()
            reloadData()
        }
    }

    companion object {
        const val h = 300f
    }

}