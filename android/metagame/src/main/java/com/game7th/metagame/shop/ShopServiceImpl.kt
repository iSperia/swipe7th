package com.game7th.metagame.shop

import com.game7th.metagame.PersistentStorage
import com.game7th.metagame.account.AccountService
import com.game7th.metagame.account.RewardData
import com.game7th.metagame.account.dto.Currency
import com.game7th.metagame.inventory.GearService
import com.game7th.metagame.shop.dto.PaymentOption
import com.game7th.metagame.shop.dto.ShopItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class ShopServiceImpl(
        private val storage: PersistentStorage,
        private val gearService: GearService,
        private val accountService: AccountService,
        private val gson: Gson
) : ShopService {

    private var cachedGearItems: List<ShopItem> = storage.get(KEY_SHOP_ITEMS)?.let { shopItemsText ->
        val token = object : TypeToken<List<ShopItem.GearShopItem>>() {}.type
        gson.fromJson<List<ShopItem.GearShopItem>>(shopItemsText, token)
    } ?: emptyList()

    init {
        checkShopRefreshed()
    }

    private fun checkShopRefreshed() {
        val time = System.currentTimeMillis()
        val timeForUpdate = storage.get(KEY_NEXT_TIME_SHOP_UPDATE)?.toLong() ?: 0
        if (time > timeForUpdate) {
            storage.put(KEY_NEXT_TIME_SHOP_UPDATE, (time + (SHOP_UPDATE_TIME - time % SHOP_UPDATE_TIME)).toString())
            //update shop
            cachedGearItems = listOf(1, 3, 5, 10, 20, 30, 50, 100).map {
                gearService.getArtifactReward(it)?.item?.let { item ->
                    ShopItem.GearShopItem(item, listOf(
                            PaymentOption(item.level * 500, Currency.GOLD),
                            PaymentOption(item.level * 10, Currency.GEMS)
                    ), UUID.randomUUID().toString())
                } ?: null
            }.filterNotNull()
            storage.put(KEY_SHOP_ITEMS, gson.toJson(cachedGearItems))
        }
    }

    override fun listItems(): List<ShopItem> {
        checkShopRefreshed()
        return cachedGearItems
    }

    override fun acquireItem(id: String, paymentOption: PaymentOption): Boolean {
        cachedGearItems.firstOrNull { it.id == id }?.let { shopItem ->
            when (shopItem) {
                is ShopItem.GearShopItem -> {
                    if (shopItem.paymentOptions.contains(paymentOption)) {
                        val balance = accountService.getBalance()
                        if (balance.currencies[paymentOption.currency] ?: 0 >= paymentOption.amount) {
                            accountService.spend(paymentOption.currency, paymentOption.amount)
                            gearService.addRewards(listOf(RewardData.ArtifactRewardData(shopItem.item)))

                            cachedGearItems = cachedGearItems.filter { it.id != id }
                            storage.put(KEY_SHOP_ITEMS, gson.toJson(cachedGearItems))
                            return true
                        }
                    }
                }
            }
        }
        return false
    }

    companion object {
        const val KEY_NEXT_TIME_SHOP_UPDATE = "shop.nextupdate"
        const val KEY_SHOP_ITEMS = "shop.items"
        const val SHOP_UPDATE_TIME = 60 * 60 * 1000L
    }
}