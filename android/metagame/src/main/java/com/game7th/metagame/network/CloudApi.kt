package com.game7th.metagame.network

import com.game7th.metagame.CloudEnvironment
import com.game7th.swiped.api.*
import com.game7th.swiped.api.battle.*
import com.game7th.swiped.api.battle.protocol.Protocol
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
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext

enum class NetworkErrorStatus {
    CONNECTION_ERROR,
    UNAUTHORIZED,
    UNKNOWN_ERROR
}

data class NetworkError(
        val status: NetworkErrorStatus,
        val details: String,
        val errorCause: Throwable? = null
) : RuntimeException("Network error $status", errorCause)

class CloudApi(
        private val environment: CloudEnvironment,
        private val instanceId: String) : CoroutineScope {

    override val coroutineContext: CoroutineContext = newFixedThreadPoolContext(2, "cloud")

    var token: String? = null

    val json = defaultSerializer()

    val gson = Gson()

    private val baseUrl = environment.endpoint

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
                socketTimeout = 35000
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
        expectSuccess = true
        HttpResponseValidator {
            validateResponse { response ->
                val status = response.status.value
                when (status) {
                    in 300..399 -> throw NetworkError(NetworkErrorStatus.CONNECTION_ERROR, response.readText())
                    in 400..499 -> throw NetworkError(NetworkErrorStatus.CONNECTION_ERROR, response.readText())
                    in 500..599 -> throw NetworkError(NetworkErrorStatus.CONNECTION_ERROR, response.readText())
                }
                if (status >= 600) throw NetworkError(NetworkErrorStatus.UNKNOWN_ERROR, response.readText())
            }
            handleResponseException { e ->
                if (e !is NetworkError) {
                    throw NetworkError(NetworkErrorStatus.CONNECTION_ERROR, "Connection failure", e)
                } else {
                    throw e
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

    suspend fun markLocationComplete(actName: String, locationId: Int, personageId: String): LocationCompleteResponseDto = client.post("$baseUrl/acts/$actName/$locationId/markComplete?personageId=$personageId") { sign() }

    suspend fun getActProgress(actName: String): List<LocationProgressDto> = client.get("$baseUrl/acts/$actName/progress") { sign() }

    suspend fun getGearInfo(items: List<String>): List<InventoryItemFullInfoDto> = client.get("$baseUrl/gear/info") {
        body = json.write(items)
    }

    suspend fun getInventory(): InventoryPoolDto = client.get<InventoryPoolDto>("$baseUrl/gear/inventory") { sign() }

    suspend fun putItemOn(personageId: String, item: InventoryItemFullInfoDto): Unit = client.post("$baseUrl/gear/putOn?personageId=$personageId&itemId=${item.id}") { sign() }

    suspend fun putItemOff(personageId: String, item: InventoryItemFullInfoDto): Unit = client.post("$baseUrl/gear/putOff?personageId=$personageId&itemId=${item.id}") { sign() }

    suspend fun consumeFlask(itemId: String): Unit = client.post("$baseUrl/gear/consumeFlask?flaskId=$itemId") { sign() }

    suspend fun listShopDisplay(): ShopDisplayDto = client.get("$baseUrl/shop/display") { sign() }

    suspend fun validateGooglePurchase(token: String, shopItemId: String): List<PackEntryDto> = client.post("$baseUrl/shop/google_inapp_purchase?token=$token&shopItemId=$shopItemId") { sign() }

    suspend fun getPersonageGearedStats(personageId: String): PersonageAttributesDto = client.get("$baseUrl/account/personages/$personageId") { sign() }

    suspend fun internalPurchase(request: PurchaseRequestDto): List<PackEntryDto> = client.post("$baseUrl/shop/acquire") {
        body = request
        sign()
    }

    suspend fun encounterLocation(actId: String, locationId: Int, personageId: String): String = client.post("$baseUrl/encounter?actId=$actId&locationId=$locationId&personageId=$personageId") { sign() }

    suspend fun getStringPackVersion(packName: String): Int = client.get("$baseUrl/strings/$packName/version")

    suspend fun getStringPack(packName: String): List<TextEntryDto> = client.get("$baseUrl/strings/$packName")

    suspend fun connectBattle(accountId: String, battleId: String, outFlow: Flow<InputBattleEvent>, handler: suspend (BattleEvent) -> Unit) {
        val socket = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().let {
            it.connect(InetSocketAddress(baseUrl.replace("http://", "").replace(":8080", ""), environment.socketPort)) {
                noDelay = true
                keepAlive = true
            }
        }
        val input = socket.openReadChannel()
        val output = socket.openWriteChannel(autoFlush = true)

        val outputRoutine = launch {
            try {
                output.writeStringUtf8("$accountId\n")
                output.writeStringUtf8("$battleId\n")
                outFlow.collect { event ->
                    val builder = Protocol.BattleMessage.newBuilder().apply {
                        when (event) {
                            is InputBattleEvent.ConcedeBattleEvent -> {
                                messageType = Protocol.BattleMessage.MessageType.CONCEDE
                            }
                            is InputBattleEvent.FlaskBattleEvent -> {
                                messageType = Protocol.BattleMessage.MessageType.FLASK
                                skin = event.flaskId
                            }
                            is InputBattleEvent.SwipeBattleEvent -> {
                                messageType = Protocol.BattleMessage.MessageType.SWIPE
                                dx = event.dx
                                dy = event.dy
                            }
                            is InputBattleEvent.PlayerReadyEvent -> {
                                messageType = Protocol.BattleMessage.MessageType.READY
                            }
                            is InputBattleEvent.HeartBeatEvent -> {
                                messageType = Protocol.BattleMessage.MessageType.HEARTBEAT
                            }
                        }
                    }
                    val data = builder.build().toByteArray()
                    println("S7TH-WS-OUT: ${event.javaClass.simpleName} ${data.toHexString()}")
                    output.writeInt(data.size)
                    output.writeFully(ByteBuffer.wrap(data))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val inputRoutine = launch {
            while (!socket.isClosed) {
                val size = input.readInt()
                val buffer = ByteBuffer.allocate(size)
                input.readFully(buffer)
                val message = Protocol.BattleMessage.parseFrom(buffer.array())
                println("S7TH-IN: ${message.messageType}")
                val event = when (message.messageType) {
                    Protocol.BattleMessage.MessageType.BATTLE_READY -> BattleEvent.BattleReadyEvent(
                            BattleResourcesDto(message.resourcesList.resourcesList.toList())
                    )
                    Protocol.BattleMessage.MessageType.FLASK_CONSUMED -> BattleEvent.FlaskConsumedEvent("")
                    Protocol.BattleMessage.MessageType.HEARTBEAT_RESPONSE -> BattleEvent.HeartbeatResponse("")
                    Protocol.BattleMessage.MessageType.CREATE_TILE -> BattleEvent.CreateTileEvent("",
                            message.tileViewModel.let {
                                TileViewModel(it.id, it.skin, it.stackSize, it.maxStackSize, it.stun)
                            },
                            message.position,
                            message.sourcePosition
                    )
                    Protocol.BattleMessage.MessageType.SWIPE_MOTION -> BattleEvent.SwipeMotionEvent("",
                            message.tileFieldEventsList.map { tileEvent ->
                                TileFieldEvent(
                                        when (tileEvent.tileFieldEventType) {
                                            Protocol.BattleMessage.TileFieldEvent.TileFieldEventType.MOVE -> TileFieldEventType.MOVE
                                            Protocol.BattleMessage.TileFieldEvent.TileFieldEventType.MERGE -> TileFieldEventType.MERGE
                                            else -> TileFieldEventType.MOVE
                                        },
                                        tileEvent.id, tileEvent.position, if (tileEvent.hasTile()) tileEvent.tile.let {
                                    TileViewModel(it.id, it.skin, it.stackSize, it.maxStackSize, it.stun)
                                } else null
                                )
                            }
                    )
                    Protocol.BattleMessage.MessageType.COMBO_UPDATE -> BattleEvent.ComboUpdateEvent("", message.id)
                    Protocol.BattleMessage.MessageType.UPDATE_TILE -> BattleEvent.UpdateTileEvent("", message.id,
                            message.tileViewModel.let { TileViewModel(it.id, it.skin, it.stackSize, it.maxStackSize, it.stun) })
                    Protocol.BattleMessage.MessageType.REMOVE_TILE -> BattleEvent.RemoveTileEvent("", message.id)
                    Protocol.BattleMessage.MessageType.SHOW_TILE -> BattleEvent.ShowTileEffect("", message.position, message.effect)
                    Protocol.BattleMessage.MessageType.CREATE_PERSONAGE -> BattleEvent.CreatePersonageEvent(
                            message.personageViewModel.let { createPersonageViewModel(it) },
                            message.position,
                            message.appearStrategy
                    )
                    Protocol.BattleMessage.MessageType.PERSONAGE_ATTACK -> BattleEvent.PersonageAttackEvent(
                            createPersonageViewModel(message.personageViewModel),
                            message.targetsList.map { createPersonageViewModel(it) },
                            message.attackIndex
                    )
                    Protocol.BattleMessage.MessageType.PERSONAGE_POSITIONED_ABILITY -> BattleEvent.PersonagePositionedAbilityEvent(
                            createPersonageViewModel(message.personageViewModel), message.id, message.attackIndex
                    )
                    Protocol.BattleMessage.MessageType.PERSONAGE_DAMAGE -> BattleEvent.PersonageDamageEvent(
                            createPersonageViewModel(message.personageViewModel), message.amount
                    )
                    Protocol.BattleMessage.MessageType.PERSONAGE_DEAD -> BattleEvent.PersonageDeadEvent(
                            createPersonageViewModel(message.personageViewModel), message.blocking)
                    Protocol.BattleMessage.MessageType.PERSONAGE_UPDATE -> BattleEvent.PersonageUpdateEvent(
                            createPersonageViewModel(message.personageViewModel)
                    )
                    Protocol.BattleMessage.MessageType.PERSONAGE_HEAL -> BattleEvent.PersonageHealEvent(
                            createPersonageViewModel(message.personageViewModel), message.amount
                    )
                    Protocol.BattleMessage.MessageType.SHOW_AILMENT -> BattleEvent.ShowAilmentEffect(message.id, message.skin)
                    Protocol.BattleMessage.MessageType.REMOVE_PERSONAGE -> BattleEvent.RemovePersonageEvent(message.id)
                    Protocol.BattleMessage.MessageType.NEW_WAVE -> BattleEvent.NewWaveEvent(message.id)
                    Protocol.BattleMessage.MessageType.SHOW_SPEECH -> message.speechMessage.let {
                        BattleEvent.ShowSpeech(
                                it.portrait, it.text, it.name
                        )
                    }
                    Protocol.BattleMessage.MessageType.VICTORY -> BattleEvent.VictoryEvent
                    Protocol.BattleMessage.MessageType.DEFEAT -> BattleEvent.DefeatEvent
                    Protocol.BattleMessage.MessageType.WISDOM_UPDATE -> BattleEvent.WisdomUpdateEvent("", message.id)
                    else -> null
                }
                event?.let { event ->
                    println("S7TH-WS-IN: ${event.javaClass.simpleName}")
                    handler.invoke(event)
                }
            }
        }
        inputRoutine.join()
        outputRoutine.cancelAndJoin()
        println("S7TH: CLOSE CONNECTION")
    }

    fun createPersonageViewModel(builder: Protocol.BattleMessage.PersonageViewModel) = UnitViewModel(
            builder.personageStats.let { UnitStatsDto(it.body, it.health, it.maxHealth, it.armor, it.spirit, it.regeneration, it.evasion, it.mind, it.wisdom, it.resist, it.resistMax, it.level, it.tick, it.maxTick, it.tickAbility.let { if (it.isEmpty()) null else it }, it.isStunned, it.isFrozen) },
            builder.skin,
            builder.portrait,
            builder.id,
            builder.team
    )

    fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

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