package com.game7th.metagame.inventory

import com.game7th.metagame.PersistentStorage
import com.game7th.metagame.network.CloudApi
import com.game7th.metagame.network.NetworkError
import com.game7th.swiped.api.FlaskItemFullInfoDto
import com.game7th.swiped.api.InventoryItemFullInfoDto
import com.google.gson.Gson

class GearServiceImpl(
        private val api: CloudApi
) : GearService {

    var itemsCache: List<InventoryItemFullInfoDto> = emptyList()

    var flaskCache: List<FlaskItemFullInfoDto> = emptyList()

    var itemsDirty = true

    override suspend fun listInventory(): List<InventoryItemFullInfoDto> {
        maybeReloadData()
        return itemsCache
    }

    override suspend fun listFlasks(): List<FlaskItemFullInfoDto> {
        maybeReloadData()
        return flaskCache
    }

    private suspend fun maybeReloadData() {
        if (itemsDirty) {
            val pool = api.getInventory()
            itemsCache = pool.items
            flaskCache = pool.flasks
            itemsDirty = false
        }
    }

    override suspend fun equipItem(personageId: String, item: InventoryItemFullInfoDto) {
        api.putItemOn(personageId, item)
        itemsDirty = true
    }

    override suspend fun dequipItem(personageId: String, item: InventoryItemFullInfoDto) {
        api.putItemOff(personageId, item)
        itemsDirty = true
    }

    override suspend fun reloadData() {
        itemsDirty = true
    }

    override suspend fun dustItem(item: InventoryItemFullInfoDto): Boolean {
        try {
            api.dustItem(item)
            reloadData()
            return true
        } catch (e: NetworkError) {
            return false
        }
    }

    override suspend fun pumpItem(item: InventoryItemFullInfoDto): Boolean {
        try {
            api.pumpItem(item)
            reloadData()
            return true
        } catch (e: NetworkError) {
            return false
        }
    }

    override suspend fun consumeFlask(flaskItemId: String) {
        api.consumeFlask(flaskItemId)
        //Positive prediction
        flaskCache = flaskCache.mapNotNull {
            if (it.id == flaskItemId) {
                if (it.stackSize > 1) it.copy(stackSize = it.stackSize - 1) else null
            } else it
        }
    }
}