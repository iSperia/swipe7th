package com.game7th.swipe.campaign

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Align
import com.game7th.metagame.PersistentStorage
import com.game7th.metagame.campaign.*
import com.game7th.metagame.campaign.dto.ActConfig
import com.game7th.metagame.campaign.dto.CampaignNodeType
import com.game7th.metagame.campaign.dto.LocationConfig
import com.game7th.swipe.BaseScreen
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.SwipeGameGdx
import com.game7th.swipe.alchemy.AlchemyPanel
import com.game7th.swipe.alchemy.AlchemyPanelMode
import com.game7th.swipe.campaign.bottom_menu.BottomMenu
import com.game7th.swipe.campaign.party.PartyView
import com.game7th.swipe.campaign.prepare.BattlePrepareDialog
import com.game7th.swipe.campaign.top_menu.CurrencyPanel
import com.game7th.swipe.dialog.DismissStrategy
import com.game7th.swipe.shop.ShopPanel
import com.game7th.swipe.util.animateHideToBottom
import com.game7th.swipe.util.animateShowFromBottom
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import ktx.async.KtxAsync
import kotlin.math.*

sealed class UiState {
    object Hidden : UiState()
    object PartyUi : UiState()
    object ShopUi : UiState()
    object AlchUi : UiState()
    data class BattlePreparation(
            val node: LocationConfig
    ) : UiState()
}

/**
 * The screen for single campaign
 */
@Suppress("NAME_SHADOWING")
class ActScreen(
        game: SwipeGameGdx,
        private val actsService: ActsService,
        private val actId: String,
        context: GdxGameContext,
        private val storage: PersistentStorage
) : BaseScreen(context, game) {

    private val batch = SpriteBatch()
    private lateinit var actConfig: ActConfig
    private lateinit var backgroundTexture: TextureRegion

    private var circleScale: Float = 0f
    private var circleOffset: Float = 0f
    private var lockScale: Float = 0f
    private var lockOffset: Float = 0f

    private val linkRenderer = ShapeRenderer()
    private val linkMainColor = Color(0.1f, 0.1f, 0.1f, 0.8f)
    private val linkSubColor = Color(0.1f, 0.1f, 0.1f, 0.5f)

    private var mapBottomOffset = 0f
    private var scroll = 0f
    private var scrollImpulse = 0f
    lateinit var gestureDetector: GestureDetector

    lateinit var bottomMenu: BottomMenu
    lateinit var currencyView: CurrencyPanel
    lateinit var actAtlas: TextureAtlas

    private var uiState: UiState = UiState.Hidden

    private var battlePrepareDialog: BattlePrepareDialog? = null
    private var partyUi: PartyView? = null
    private var shopUi: ShopPanel? = null
    private var alchUi: AlchemyPanel? = null

    var isScrollEnabled = true

    lateinit var backgroundMusic: Music

    private var readyToRender = false

    override fun show() {
        super.show()
        KtxAsync.launch {

            actConfig = actsService.getActConfig(actId)

            actAtlas = TextureAtlas(Gdx.files.internal("textures/acts/${actId}.atlas"))
            backgroundTexture = actAtlas.findRegion("bg")

            gestureDetector = GestureDetector(game.width / 12f, 0.4f, 1.1f, Float.MAX_VALUE, object : GestureDetector.GestureAdapter() {
                override fun pan(x: Float, y: Float, deltaX: Float, deltaY: Float): Boolean {
                    if (isScrollEnabled) {
                        closeOverlay()
                        scrollImpulse = 0f
                        scroll += deltaY
                        normalizeScroll()
                    }
                    return super.pan(x, y, deltaX, deltaY)
                }

                override fun fling(velocityX: Float, velocityY: Float, button: Int): Boolean {
                    if (isScrollEnabled) {
                        closeOverlay()
                        scrollImpulse = velocityY
                    }
                    return super.fling(velocityX, velocityY, button)
                }

                override fun tap(x: Float, y: Float, count: Int, button: Int): Boolean {
                    if (isFocusShown()) return false
                    closeOverlay()
                    val wy = (game.height - y + scroll - mapBottomOffset) / game.scale
                    val wx = x / game.scale
                    actConfig.nodes.forEach { node ->
                        val rect = Rectangle(node.x - 30, node.y - 30, 60f, 60f)
                        if (rect.contains(wx, wy) && !node.isLocked) {
                            transiteUiState(UiState.BattlePreparation(node))
                            return true
                        }
                    }
                    return super.tap(x, y, count, button)
                }
            })
            game.multiplexer.addProcessor(1, gestureDetector)

            val texture = getTextureForCircle(CampaignNodeType.FARM)
            circleScale = game.width / 8 / texture.regionWidth
            circleOffset = game.width / 16

            val lockTexture = context.commonAtlas.findRegion("lock")
            lockScale = game.width / 10 / lockTexture.regionWidth
            lockOffset = game.width / 20

            bottomMenu = BottomMenu(context).apply {
                onPartyButtonPressed = this@ActScreen::onPartyButtonPressed
                onShopButtonPressed = this@ActScreen::onShopButtonPressed
                onAlchButtonPressed = this@ActScreen::onAlchButtonPressed
            }
            stage.addActor(bottomMenu)

            currencyView = CurrencyPanel(context, game.accountService).apply {
                y = game.height - 34f * context.scale
            }
            stage.addActor(currencyView)

            bottomMenu.zIndex = 100
            mapBottomOffset = context.scale * 48f

            backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("act0_theme.ogg")).apply {
                volume = 0.5f
                isLooping = true
                play()
            }

            readyToRender = true

            async {
                game.accountService.refreshBalance()
                currencyView.refreshBalance()
            }
        }
    }

    private fun normalizeScroll() {
        scroll = max(0f, min(scroll, game.scale * backgroundTexture.regionHeight - game.height + mapBottomOffset))
    }

    private fun onPartyButtonPressed() {
        if (uiState == UiState.PartyUi) return
        transiteUiState(UiState.PartyUi)
    }

    private fun onShopButtonPressed() {
        if (uiState == UiState.ShopUi) return
        transiteUiState(UiState.ShopUi)
    }

    private fun onAlchButtonPressed() {
        if (uiState == UiState.AlchUi) return
        transiteUiState(UiState.AlchUi)
    }

    private fun closeOverlay() {
        transiteUiState(UiState.Hidden)
    }

    override fun render(delta: Float) {
        if (!readyToRender) return
        if (scrollImpulse != 0f) {
            scroll += scrollImpulse * delta
            scrollImpulse *= 1 - 2f * delta
            normalizeScroll()
            if (abs(scrollImpulse) < 5f) {
                scrollImpulse = 0f
            }
        }

        batch.begin()
        batch.draw(backgroundTexture, 0f, -scroll + mapBottomOffset, backgroundTexture.regionWidth * game.scale, backgroundTexture.regionHeight * game.scale)
        batch.end()

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        linkRenderer.begin(ShapeRenderer.ShapeType.Filled)
        actConfig.nodes.forEach {
            it.drawLinks()
        }
        linkRenderer.end()
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin()
        actConfig.nodes.forEach {
            it.draw(batch)
        }
        batch.end()

        super.render(delta)
    }

    override fun resize(width: Int, height: Int) {
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun hide() {
        backgroundMusic.stop()
        backgroundMusic.dispose()
        stage.dispose()
        game.multiplexer.removeProcessor(gestureDetector)
        game.multiplexer.removeProcessor(stage)
        actAtlas.dispose()
        batch.dispose()
    }

    override fun dispose() {
    }

    private fun LocationConfig.drawLinks() {
        unlock.forEach { index ->
            actConfig.findNode(index)?.let { targetNode ->
                val x1 = x * game.scale
                val x2 = targetNode.x * game.scale
                val y1 = y * game.scale - scroll + mapBottomOffset
                val y2 = targetNode.y * game.scale - scroll + mapBottomOffset
                linkRenderer.color = linkSubColor
                linkRenderer.rectLine(x1, y1, x2, y2, game.width * 6f / 480f)
                linkRenderer.color = linkMainColor
                linkRenderer.rectLine(x1, y1, x2, y2, game.width * 3f / 480f)
            }
        }
    }

    private fun LocationConfig.draw(batch: SpriteBatch) {
        val texture = getTextureForCircle(type)
        batch.draw(texture,
                game.scale * x - circleOffset,
                game.scale * y - circleOffset - scroll + mapBottomOffset,
                0f,
                0f,
                texture.regionWidth.toFloat(),
                texture.regionHeight.toFloat(),
                circleScale,
                circleScale,
                0f)

        if (isLocked) {
            val texture = context.commonAtlas.findRegion("lock")
            batch.draw(texture,
                    game.scale * x - lockOffset,
                    game.scale * y - lockOffset - scroll + mapBottomOffset,
                    0f,
                    0f,
                    texture.regionWidth.toFloat(),
                    texture.regionHeight.toFloat(),
                    lockScale,
                    lockScale,
                    0f
            )
        }

        if (type == CampaignNodeType.FARM) {
            val now = System.currentTimeMillis()
            val delta = max(0L, (timeoutStart + farmConfig!!.timeout * 1000L - now)/1000L + 1)
//            if (delta > 0) {
                context.regularFont.setColor(0f, 0f, 0f, 1f)
                context.regularFont.draw(batch, delta.toString(), game.scale * x - lockOffset, game.scale * y - scroll + mapBottomOffset, texture.regionWidth.toFloat(), Align.center, false)
                context.regularFont.setColor(1f, 1f, 1f, 1f)
//            }
        }
    }

    private fun getTextureForCircle(type: CampaignNodeType) = when (type) {
        CampaignNodeType.BOSS -> context.commonAtlas.findRegion("circle_red")
        CampaignNodeType.FARM -> context.commonAtlas.findRegion("circle_orange")
        CampaignNodeType.REGULAR -> context.commonAtlas.findRegion("circle_blue")
    }

    private fun transiteUiState(state: UiState) {
        //First of all, hide previous
        when (this.uiState) {
            is UiState.PartyUi -> hidePartyUi()
            is UiState.BattlePreparation -> hideBattlePreparation()
            is UiState.ShopUi -> hideShopUi()
            is UiState.AlchUi -> hideAlchUi()
        }

        uiState = state

        //Now, show stuff
        when (state) {
            is UiState.BattlePreparation -> showBattlePreparation(state.node)
            is UiState.PartyUi -> showPartyUi()
            is UiState.ShopUi -> showShopUi()
            is UiState.AlchUi -> showAlchUi()
        }
    }

    private fun hideAlchUi() {
        alchUi?.animateHideToBottom(context, AlchemyPanel.h).also { alchUi = null }
    }

    private fun hideShopUi() {
        shopUi?.animateHideToBottom(context, ShopPanel.h).also { shopUi = null }
    }

    private fun hidePartyUi() {
        partyUi?.animateHide().also { partyUi = null }
    }

    private fun showAlchUi() {
        alchUi = AlchemyPanel(context, game.gearService, AlchemyPanelMode.CraftMode).apply {
            y = context.scale * 48f
        }
        stage.addActor(alchUi)
        alchUi?.zIndex = 0
        alchUi?.animateShowFromBottom(context, AlchemyPanel.h)
    }

    private fun showShopUi() {
        shopUi = ShopPanel(context, this@ActScreen, game.shopService, currencyView::refreshBalance).apply {
            y = context.scale * 48f
        }
        stage.addActor(shopUi)
        shopUi?.zIndex = 0
        shopUi?.animateShowFromBottom(context, ShopPanel.h)
    }

    private fun showPartyUi() {
        partyUi = PartyView(context, game.accountService, game.gearService).apply {
            y = context.scale * 48f
        }
        stage.addActor(partyUi)
        partyUi?.zIndex = 0
    }

    private fun showBattlePreparation(node: LocationConfig) {
        battlePrepareDialog = BattlePrepareDialog(
                game = game,
                accountId = game.accountService.getAccountId(),
                context = context,
                actId = this@ActScreen.actId,
                locationId = node.id,
                config = node,
                actsService = actsService,
                shownCallback = {}) {}
        stage.addActor(battlePrepareDialog)
        battlePrepareDialog?.zIndex = max(0, stage.actors.size - 3)
    }

    private fun hideBattlePreparation() {
        battlePrepareDialog?.let { dialog ->
            dialog.remove()
            battlePrepareDialog = null
        }
    }

    override fun showFocusView(text: String, rect: Rectangle, strategy: DismissStrategy, dismissCallback: (() -> Unit)?) {
        isScrollEnabled = false
        super.showFocusView(text, rect, strategy, dismissCallback)
    }

    override fun currencyUpdated() {
        super.currencyUpdated()
        currencyView.refreshBalance()
    }

    override fun personagesUpdated() {
        super.personagesUpdated()
        KtxAsync.launch { game.accountService.refreshPersonages() }
    }

    override fun inventoryUpdated() {
        super.inventoryUpdated()
        KtxAsync.launch {
            game.gearService.reloadData()
        }
    }
}