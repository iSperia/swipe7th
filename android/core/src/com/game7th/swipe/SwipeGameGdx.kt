package com.game7th.swipe

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.game7th.battle.balance.SwipeBalance
import com.game7th.metagame.FileProvider
import com.game7th.metagame.GameService
import com.game7th.metagame.PersistentStorage
import com.game7th.metagame.account.AccountService
import com.game7th.metagame.account.AccountServiceImpl
import com.game7th.metagame.campaign.ActConfig
import com.game7th.metagame.campaign.ActsService
import com.game7th.metagame.campaign.ActsServiceImpl
import com.game7th.swipe.campaign.ActScreen
import com.google.gson.Gson
import ktx.async.KtxAsync
import java.io.File

class SwipeGameGdx(private val storage: PersistentStorage) : Game() {

    val multiplexer = InputMultiplexer()
    val gson = Gson()

    lateinit var context: GdxGameContext
    lateinit var actService: ActsService
    lateinit var accountService: AccountService
    lateinit var service: GameService
    lateinit var screenContext: ScreenContext

    lateinit var uiAtlas: TextureAtlas

    var width = 0f
    var height = 0f
    var scale = 0f

    override fun create() {
        KtxAsync.initiate()

        initializeActService()
        initializeAccountService()
        service = GameService(actService)

        width = Gdx.graphics.width.toFloat()
        height = Gdx.graphics.height.toFloat()
        scale = Gdx.graphics.width / 480f

        val atlas = TextureAtlas(Gdx.files.internal("pack_0.atlas"))
        val font = BitmapFont(Gdx.files.internal("atarian.fnt"), Gdx.files.internal("atarian_0.png"), false).apply {
            color = Color.WHITE
        }
        val balanceFile = Gdx.files.internal("balance.json")
        val balanceText = balanceFile.readString()
        val balance = Gson().fromJson<SwipeBalance>(balanceText, SwipeBalance::class.java)
        context = GdxGameContext(atlas, font, balance, scale)

        Gdx.input.inputProcessor = multiplexer

        uiAtlas = TextureAtlas(Gdx.files.internal("ui.atlas"))
        screenContext = ScreenContext(uiAtlas, font, atlas, scale)
        setScreen(ActScreen(this, actService, 0, screenContext))
    }

    private fun initializeActService() {
        actService = ActsServiceImpl(gson, storage, object : FileProvider {
            override fun getFileContent(name: String): String? {
                return Gdx.files.internal(name).readString()
            }
        })
    }

    private fun initializeAccountService() {
        accountService = AccountServiceImpl(gson, storage)
    }

    override fun render() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
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
}