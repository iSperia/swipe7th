package com.game7th.swipe

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.game7th.battle.dto.SwipeBalance
import com.game7th.metagame.FileProvider
import com.game7th.metagame.GameService
import com.game7th.metagame.PersistentStorage
import com.game7th.metagame.account.AccountService
import com.game7th.metagame.account.AccountServiceImpl
import com.game7th.metagame.campaign.ActsService
import com.game7th.metagame.campaign.ActsServiceImpl
import com.game7th.metagame.inventory.GearService
import com.game7th.metagame.inventory.GearServiceImpl
import com.game7th.metagame.shop.ShopService
import com.game7th.metagame.shop.ShopServiceImpl
import com.game7th.swipe.campaign.ActScreen
import com.game7th.metagame.network.CloudApi
import com.game7th.metagame.network.NetworkError
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import ktx.async.KtxAsync

class SwipeGameGdx(
        val storage: PersistentStorage,
        val installationId: String,
        val endpoint: String) : Game() {

    val multiplexer = InputMultiplexer()
    val gson = Gson()

    val api: CloudApi = CloudApi(endpoint, installationId)

    var servicesInitialized = false
    lateinit var context: GdxGameContext
    lateinit var gearService: GearService
    lateinit var actService: ActsService
    lateinit var shopService: ShopService
    lateinit var accountService: AccountService
    lateinit var service: GameService

    lateinit var uiAtlas: TextureAtlas

    var width = 0f
    var height = 0f
    var scale = 0f

    private val fileProvider = object : FileProvider {
        override fun getFileContent(name: String): String? {
            return Gdx.files.internal(name).readString()
        }
    }

    override fun create() {
        KtxAsync.initiate()

//        showGameScreen()

        KtxAsync.launch {
            val token = storage.get(KEY_TOKEN)
            var needRequestToken = false
            var tokenReceived = false
            if (!token.isNullOrEmpty()) {
                api.token = token
                //validate token
                try {
                    api.validateToken()
                } catch (e: NetworkError) {
                    storage.put(token, "")
                    needRequestToken = true
                }
            } else {
                needRequestToken = true
            }

            if (needRequestToken) {
                try {
                    val token = api.requestToken().token
                    api.token = token
                    storage.put(KEY_TOKEN, token)
                    tokenReceived = true
                } catch (e: NetworkError) {
                    e.printStackTrace()
                    tokenReceived = false
                }
            } else {
                tokenReceived = true
            }

            if (tokenReceived) {
                showGameScreen()
            }
       }
    }

    private fun showGameScreen() {
        initializeServices()
        service = GameService(actService)

        width = Gdx.graphics.width.toFloat()
        height = Gdx.graphics.height.toFloat()
        scale = Gdx.graphics.width / 480f

        val uiAtlas = TextureAtlas(Gdx.files.internal("ui.atlas"))
        val atlas = TextureAtlas(Gdx.files.internal("pack_0.atlas"))
        val font = BitmapFont(Gdx.files.internal("atarian.fnt"), Gdx.files.internal("atarian_0.png"), false).apply {
            color = Color.WHITE
        }
        val font2 = BitmapFont(Gdx.files.internal("anglodavek.fnt"), Gdx.files.internal("anglodavek_0.png"), false).apply {
            color = Color.WHITE
        }
        val balanceFile = Gdx.files.internal("balance.json")
        val balanceText = balanceFile.readString()
        val balance = Gson().fromJson<SwipeBalance>(balanceText, SwipeBalance::class.java)

        val textsFile = Gdx.files.internal("strings.json")
        val textsText = textsFile.readString()
        val token = object : TypeToken<Map<String, String>>() {}.type
        val texts = gson.fromJson<Map<String, String>>(textsText, token)

        context = GdxGameContext(atlas, uiAtlas, font, font2, balance, scale, texts, storage)


        Gdx.input.inputProcessor = multiplexer

        setScreen(ActScreen(this@SwipeGameGdx, actService, 0, context, storage))
    }

    private fun initializeServices() {
        if (!servicesInitialized) {
            initializeGearService()
            initializeAccountService()
            initializeActService()
            initializeShopService()
            servicesInitialized = true
        }
    }

    private fun initializeGearService() {
        gearService = GearServiceImpl(gson, storage, fileProvider)
    }

    private fun initializeActService() {
        actService = ActsServiceImpl(gson, storage, fileProvider, gearService, accountService)
    }

    private fun initializeShopService() {
        shopService = ShopServiceImpl(storage, gearService, accountService, gson)
    }

    private fun initializeAccountService() {
        accountService = AccountServiceImpl(gson, storage, fileProvider, gearService)
    }

    override fun render() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT or (if (Gdx.graphics.getBufferFormat().coverageSampling) GL20.GL_COVERAGE_BUFFER_BIT_NV else 0))
//        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        super.render()
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun dispose() {
        super.dispose()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
    }

    fun switchScreen(screen: Screen) {
        this.screen.dispose()
        setScreen(screen)
    }

    companion object {
        const val KEY_TOKEN = "auth.token"
    }
}