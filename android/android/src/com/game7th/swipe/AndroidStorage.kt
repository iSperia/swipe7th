package com.game7th.swipe

import android.content.Context
import com.game7th.metagame.PersistentStorage

class AndroidStorage(context: Context) : PersistentStorage {

    private val prefs = context.getSharedPreferences(PREFS_NAME, 0)

    override fun put(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun get(key: String): String? {
        return prefs.getString(key, null)
    }

    companion object {
        const val PREFS_NAME = "storage"
    }
}