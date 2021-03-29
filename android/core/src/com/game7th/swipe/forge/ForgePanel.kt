package com.game7th.swipe.forge

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.actions.MoveByAction
import com.badlogic.gdx.scenes.scene2d.actions.RunnableAction
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.utils.Align
import com.game7th.metagame.account.AccountService
import com.game7th.metagame.account.dto.Currency
import com.game7th.metagame.inventory.GearService
import com.game7th.metagame.inventory.dto.InventoryItem
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.campaign.inventory.InventoryAction
import com.game7th.swipe.campaign.inventory.InventoryDetailPanel
import com.game7th.swipe.campaign.inventory.ItemView
import ktx.actors.onClick
import kotlin.math.max
import kotlin.math.min

class ForgePanel(
        private val context: GdxGameContext,
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

    var detailPanel: InventoryDetailPanel? = null

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
            val itemView = ItemView(context, item, true, 80f * context.scale).apply {
                x = (index / 3) * 80f * context.scale
                y = (160f - (index % 3) * 80f) * context.scale
            }
            itemView.onClick {
                dismissDetailPanel()
                detailPanel = InventoryDetailPanel(
                        context,
                        item,
                        listOf(InventoryAction.IconAction("+${item.level * 50}", "ui_button_dust", Currency.DUST), InventoryAction.IconAction("-${item.level * 150}", "ui_button_levelup", Currency.DUST)),
                        this@ForgePanel::dismissDetailPanel,
                        this@ForgePanel::processAction).apply {
                    x = min(context.scale * 320f, panelScroller.x + itemView.x)
                    y = panelScroller.y + itemView.y
                }
                this@ForgePanel.addActor(detailPanel)
            }
            panelItems.addActor(itemView)
        }
        (1..emptyItems).forEach {
            val itemView = ItemView(context, null, true, 80f * context.scale).apply {
                val index = items.size + it - 1
                x = (index / 3) * 80f * context.scale
                y = (160f - (index % 3) * 80f) * context.scale
            }
            panelItems.addActor(itemView)
        }
    }

    fun processAction(item: InventoryItem, index: Int, meta: String?) {
        when (index) {
            0 -> { //to dust!
                accountService.fund(Currency.DUST, item.level * 50)
                gearService.removeItem(item)
            }
            1 -> { //level up
                val dustNeeded = 150 * item.level
                if ((accountService.getBalance().currencies[Currency.DUST] ?: 0) >= dustNeeded) {
                    accountService.spend(Currency.DUST, dustNeeded)
                    gearService.upgradeItem(item)
                }
            }
        }
        dismissDetailPanel()
        reloadData()
    }

    companion object {
        const val h = 300f
    }

}