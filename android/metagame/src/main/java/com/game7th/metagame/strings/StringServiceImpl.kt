package com.game7th.metagame.strings

import com.game7th.metagame.PersistentStorage
import com.game7th.metagame.network.CloudApi
import com.game7th.swiped.api.TextEntryDto
import com.google.gson.Gson

data class StringPackWrapper(
        val entries: List<TextEntryDto>
)

/**
 * Externalization service of strings
 */
class StringServiceImpl(
    private val storage: PersistentStorage,
    private val gson: Gson,
    private val api: CloudApi
) {

    private val activePacks = mutableSetOf<String>()

    private val texts = mutableMapOf<String, String>()

    fun getString(key: String) = texts[key] ?: key

    suspend fun loadPack(packName: String) {
        val localPackVersion = storage.get("string_pack_${packName}_version")?.toInt() ?: 0
        val remoteVersion = api.getStringPackVersion(packName)
        if (remoteVersion > localPackVersion) {
            val pack = api.getStringPack(packName)
            storage.put("string_pack_${packName}", gson.toJson(StringPackWrapper(pack)))
            storage.put("string_pack_${packName}_version", remoteVersion.toString())
            pack.forEach { texts[it.name] = it.ru }
        } else {
            val pack = gson.fromJson(storage.get("string_pack_$packName") ?: "{entries:[]}", StringPackWrapper::class.java)
            pack.entries.forEach { texts[it.name] = it.ru }
        }
    }
}