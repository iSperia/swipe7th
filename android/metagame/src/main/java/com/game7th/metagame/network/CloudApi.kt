package com.game7th.metagame.network

import com.game7th.swiped.api.*
import com.game7th.swiped.api.battle.*
import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.logging.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.content.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.mapNotNull
import kotlinx.coroutines.channels.receiveOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onEach

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

    val gson = Gson()

    private val client = HttpClient(CIO) {
        install(WebSockets)
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

    suspend fun getInventory(): InventoryPoolDto = client.get<InventoryPoolDto> ("$baseUrl/gear/inventory") { sign() }

    suspend fun putItemOn(personageId: String, item: InventoryItemFullInfoDto): Unit = client.post("$baseUrl/gear/putOn?personageId=$personageId&itemId=${item.id}") { sign() }

    suspend fun putItemOff(personageId: String, item: InventoryItemFullInfoDto): Unit = client.post("$baseUrl/gear/putOff?personageId=$personageId&itemId=${item.id}") { sign() }

    suspend fun consumeFlask(itemId: String): Unit = client.post("$baseUrl/gear/consumeFlask?flaskId=$itemId") { sign() }

    suspend fun dustItem(item: InventoryItemFullInfoDto): Unit = client.post("$baseUrl/gear/dust?itemId=${item.id}") { sign() }

    suspend fun pumpItem(item: InventoryItemFullInfoDto): Unit = client.post("$baseUrl/gear/pump?itemId=${item.id}") { sign() }

    suspend fun listShopDisplay(): ShopDisplayDto = client.get("$baseUrl/shop/display") { sign() }

    suspend fun validateGooglePurchase(token: String, shopItemId: String): List<PackEntryDto> = client.post("$baseUrl/shop/google_inapp_purchase?token=$token&shopItemId=$shopItemId") { sign() }

    suspend fun internalPurchase(request: PurchaseRequestDto): List<PackEntryDto> = client.post("$baseUrl/shop/acquire") {
        body = request
        sign()
    }

    suspend fun encounterLocation(actId: String, locationId: Int, difficulty: Int, personageId: String): BattleResultDto = client.post("$baseUrl/encounter?actId=$actId&locationId=$locationId&difficulty=$difficulty&personageId=$personageId") { sign() }

    suspend fun connectBattle(battleId: String, output: Flow<InputBattleEvent>, handler: suspend (BattleEvent) -> Unit) = client.ws(
            host = baseUrl.replace("http://", "").replace("https://", "").replace(":8080", ""),
            path = "/battle?battleId=$battleId",
            port = 8080,
            request = {
                this.headers.set("Authorization", "Bearer $token")
            }
    ) {
        val outputRoutine = launch {
            try {
                output.collect {
                    val name = InputBattleEvent.getEventDtoType(it).toString()
                    val payload = gson.toJson(it)
                    val frame = BattleFrame(name, payload)
                    val frameString = gson.toJson(frame)
                    println("S7TH-WS-OUT: $frameString")
                    outgoing.send(Frame.Text(frameString))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
        val inputRoutine = launch {
            try {
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val payload = frame.readText()
                            println("S7TH-WS-IN: $payload")
                            val frame = gson.fromJson<BattleFrame>(payload, BattleFrame::class.java)
                            val clazz = BattleEventType.valueOf(frame.name).clazz
                            val event = gson.fromJson(frame.payload, clazz)
                            handler.invoke(event)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        inputRoutine.join()
        outputRoutine.cancelAndJoin()
        close()
        println("S7TH-WS: Connection closed")
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