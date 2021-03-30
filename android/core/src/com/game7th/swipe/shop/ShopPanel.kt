package com.game7th.swipe.shop

import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.game7th.metagame.inventory.dto.InventoryItem
import com.game7th.metagame.shop.ShopService
import com.game7th.metagame.shop.dto.ShopItem
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.campaign.inventory.InventoryDetailPanel
import com.game7th.swipe.campaign.reward.CurrencyRewardView
import com.game7th.swipe.util.InventoryAction

class ShopPanel(
        private val context: GdxGameContext,
        private val shopService: ShopService,
        private val balanceRefresher: () -> Unit
): Group() {

    val bg = Image(context.uiAtlas.findRegion("ui_dialog")).apply {
        width = 480f * context.scale
        height = h * context.scale
    }

    val panelItems = Group()
    val panelScroller = ScrollPane(panelItems).apply {
        x = 10f * context.scale
        y = 10f * context.scale
        width = 460f * context.scale
        height = 310f * context.scale
    }

    lateinit var shopItems: List<ShopItem>

    init {
        addActor(bg)
        addActor(panelScroller)

        reloadData()
    }

    private fun reloadData() {
        while (panelItems.hasChildren()) panelItems.removeActorAt(0, true)
        balanceRefresher()

        shopItems = shopService.listItems()

        var offset = 0f
        shopItems.forEachIndexed { index, shopItem ->
            when (shopItem) {
                is ShopItem.GearShopItem -> {
                    val panel = InventoryDetailPanel(context, shopItem.item, shopItem.paymentOptions
                            .map {
                                InventoryAction.IconAction("-${it.amount}", CurrencyRewardView.getTextureName(it.currency), it.currency)
                            }, {}, this::processGearAcquisition).apply {
                        meta = shopItem.id
                        bg.touchable = Touchable.disabled
                        x = offset + 5f * context.scale
                    }
                    offset += panel.width + 5f * context.scale
                    panelItems.addActor(panel)
                }

                is ShopItem.PersonageShopItem -> {
                    val panel = PersonageShopPanel(context, shopItem.personage, shopItem.id, shopItem.paymentOptions.map {
                        InventoryAction.IconAction("-${it.amount}", CurrencyRewardView.getTextureName(it.currency), it.currency)
                    }, this::processPersonageAcquisition).apply {
                        bg.touchable = Touchable.disabled
                        x = offset + 5f * context.scale
                    }
                    offset += panel.width + 5f * context.scale
                    panelItems.addActor(panel)
                }
            }
        }

        panelItems.width = offset
        panelItems.height = 310f * context.scale
        panelScroller.actor = panelItems
    }

    private fun processGearAcquisition(item: InventoryItem, actionIndex: Int, meta: String?) {
        shopItems.firstOrNull { it.id == meta }?.let { shopItem ->
            when (shopItem) {
                is ShopItem.GearShopItem -> {
                    val acquireResult = shopService.acquireItem(shopItem.id, shopItem.paymentOptions[actionIndex])
                    if (acquireResult) reloadData()
                }
            }
        }
    }

    private fun processPersonageAcquisition(id: String, actionIndex: Int) {
        shopItems.firstOrNull { it.id == id }?.let { shopItem ->
            when (shopItem) {
                is ShopItem.PersonageShopItem -> {
                    val acquireResult = shopService.acquireItem(shopItem.id, shopItem.paymentOptions[actionIndex])
                    if (acquireResult) reloadData()
                }
            }
        }
    }

    companion object {
        const val h = 330f
    }
}