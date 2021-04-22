package com.game7th.metagame.network

import com.game7th.swiped.api.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.content.*
import io.ktor.http.*

enum class NetworkErrorStatus {
    CONNECTION_ERROR,
    UNKNOWN_ERROR
}

data class NetworkError(
        val status: NetworkErrorStatus,
        val details: String,
        val errorCause: Throwable? = null
) : RuntimeException("Network error $status", errorCause)

class CloudApi(
        private val baseUrl: String,
        private val instanceId: String) {

    var token: String? = null

    val json = defaultSerializer()

    private val client = HttpClient(CIO) {
        engine {
            // this: CIOEngineConfig
            maxConnectionsCount = 1000
            endpoint {
                // this: EndpointConfig
                maxConnectionsPerRoute = 100
                pipelineMaxSize = 20
                keepAliveTime = 5000
                connectTimeout = 5000
                connectAttempts = 5
            }
//            https {
//                serverName = baseUrl
//                cipherSuites = CIOCipherSuites.SupportedSuites
//                trustManager = object : X509TrustManager {
//                    override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
//                    override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
//                    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
//                }
//            }
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
        expectSuccess = false
        HttpResponseValidator {
            validateResponse { response ->
                val status = response.status.value
                when (status) {
                    else -> throw NetworkError(NetworkErrorStatus.UNKNOWN_ERROR, response.readText())
                }
            }
            handleResponseException { e ->
                if (e !is NetworkError) {
                    throw NetworkError(NetworkErrorStatus.CONNECTION_ERROR, "Connection failure", e)
                }
            }
        }
    }

    suspend fun requestToken(): AuthSuccess = client.post("$baseUrl/auth/installation") {
        body = TextContent(instanceId, ContentType.Text.Plain)
    }

    suspend fun validateToken(): String = client.get("$baseUrl/account/id") { sign() }

    suspend fun getAccount(): AccountDto = client.get("$baseUrl/account/info") { sign() }

    suspend fun getPersonages(): List<PersonageDto> = client.get("$baseUrl/account/personages") { sign() }

    suspend fun listActs(): List<ActDto> = client.get("$baseUrl/acts") { sign() }

    suspend fun getAct(actName: String): ActDto = client.get("$baseUrl/acts/$actName") { sign() }

    suspend fun markLocationComplete(actName: String, locationId: Int, difficulty: Int, personageId: String): LocationCompleteResponseDto = client.post("$baseUrl/acts/$actName/$locationId/markComplete?difficulty=$difficulty&personageId=$personageId") { sign() }

    suspend fun getActProgress(actName: String): List<LocationProgressDto> = client.get("$baseUrl/acts/$actName/progress") { sign() }

    suspend fun getGearInfo(items: List<String>): List<InventoryItemFullInfoDto> = client.get("$baseUrl/gear/info") {
        body = json.write(items)
    }

    suspend fun getInventory(): List<InventoryItemFullInfoDto> = client.get<InventoryPoolDto> ("$baseUrl/gear/inventory") { sign() }.items

    suspend fun putItemOn(personageId: String, item: InventoryItemFullInfoDto): Unit = client.post("$baseUrl/gear/putOn?personageId=$personageId&itemId=${item.id}") { sign() }

    suspend fun putItemOff(personageId: String, item: InventoryItemFullInfoDto): Unit = client.post("$baseUrl/gear/putOff?personageId=$personageId&itemId=${item.id}") { sign() }

    suspend fun dustItem(item: InventoryItemFullInfoDto): Unit = client.post("$baseUrl/gear/dust?itemId=${item.id}") { sign() }

    suspend fun pumpItem(item: InventoryItemFullInfoDto): Unit = client.post("$baseUrl/gear/pump?itemId=${item.id}") { sign() }

    suspend fun listShopDisplay(): ShopDisplayDto = client.get("$baseUrl/shop/display") { sign() }

    suspend fun validateGooglePurchase(token: String, shopItemId: String): List<PackEntryDto> = client.post("$baseUrl/shop/google_inapp_purchase?token=$token&shopItemId=$shopItemId") { sign() }

    suspend fun internalPurchase(request: PurchaseRequestDto): List<PackEntryDto> = client.post("$baseUrl/shop/acquire") {
        body = request
        sign()
    }

    private fun HttpRequestBuilder.sign() {
        headers {
            token?.let { token -> set("Authorization", "Bearer $token") }
            set("Accept", "*/*")
            set("User-Agent", "swipe-client")
            set("Content-Type", "application/json")
        }
    }

    fun dispose() {
        client.close()
    }
}