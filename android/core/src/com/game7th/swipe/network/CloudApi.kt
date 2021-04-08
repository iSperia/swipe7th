package com.game7th.swipe.network

import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.request.*

class CloudApi(private val instanceId: String) {

    val client = HttpClient(Apache)

    fun requestToken() {
        client.request<>()
    }
}