package com.game7th.metagame.shop

import com.game7th.metagame.PersistentStorage
import com.game7th.metagame.account.AccountService
import com.game7th.metagame.account.RewardData
import com.game7th.metagame.account.dto.Currency
import com.game7th.metagame.dto.UnitType
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

    private var cachedGearItems: List<ShopItem> = storage.get(KEY_GEAR_ITEMS)?.let { shopItemsText ->
        val token = object : TypeToken<List<ShopItem.GearShopItem>>() {}.type
        gson.fromJson<List<ShopItem.GearShopItem>>(shopItemsText, token)
    } ?: emptyList()

    private var cachedPersonages: List<ShopItem> = storage.get(KEY_PERSONAGES)?.let { shopItemsText ->
        val token = object : TypeToken<List<ShopItem.PersonageShopItem>>() {}.type
        gson.fromJson<List<ShopItem.PersonageShopItem>>(shopItemsText, token)
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
            cachedGearItems = listOf(1, 1, 1, 2, 2, 3).map {
                gearService.getArtifactReward(it)?.item?.let { item ->
                    ShopItem.GearShopItem(item, listOf(
                            PaymentOption(item.level * 500, Currency.GOLD),
                            PaymentOption(item.level * 10, Currency.GEMS)
                    ), UUID.randomUUID().toString())
                } ?: null
            }.filterNotNull()

            cachedPersonages = listOf(
                    ShopItem.PersonageShopItem(UnitType.POISON_ARCHER, listOf(PaymentOption(5000, Currency.GOLD), PaymentOption(500, Currency.GEMS)), UUID.randomUUID().toString())
            )
            storage.put(KEY_GEAR_ITEMS, gson.toJson(cachedGearItems))
            storage.put(KEY_PERSONAGES, gson.toJson(cachedPersonages))
        }
    }

    override fun listItems(): List<ShopItem> {
        checkShopRefreshed()
        return cachedPersonages + cachedGearItems
    }

    override fun acquireItem(id: String, paymentOption: PaymentOption): Boolean {
        listItems().firstOrNull { it.id == id }?.let { shopItem ->
            when (shopItem) {
                is ShopItem.GearShopItem -> {
                    if (shopItem.paymentOptions.contains(paymentOption)) {
                        val balance = accountService.getBalance()
                        if (balance.currencies[paymentOption.currency] ?: 0 >= paymentOption.amount) {
                            accountService.spend(paymentOption.currency, paymentOption.amount)
                            gearService.addRewards(listOf(RewardData.ArtifactRewardData(shopItem.item)))

                            cachedGearItems = cachedGearItems.filter { it.id != id }
                            storage.put(KEY_GEAR_ITEMS, gson.toJson(cachedGearItems))
                            return true
                        }
                    }
                }
                is ShopItem.PersonageShopItem -> {
                    if (shopItem.paymentOptions.contains(paymentOption)) {
                        val balance = accountService.getBalance()
                        val hasPersonage = accountService.getPersonages().firstOrNull { it.unit == shopItem.personage }
                        if (balance.currencies[paymentOption.currency] ?: 0 >= paymentOption.amount && hasPersonage == null) {
                            accountService.spend(paymentOption.currency, paymentOption.amount)
                            accountService.addPersonage(shopItem.personage)

                            cachedPersonages = cachedPersonages.filter { it.id != id }
                            storage.put(KEY_PERSONAGES, gson.toJson(cachedGearItems))
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
        const val KEY_GEAR_ITEMS = "shop.items"
        const val KEY_PERSONAGES = "shop.personages"
        const val SHOP_UPDATE_TIME = 60 * 60 * 1000L
    }
}