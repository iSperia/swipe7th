package com.game7th.metagame

interface PersistentStorage {

    fun put(key: String, value: String)

    fun get(key: String): String?
}