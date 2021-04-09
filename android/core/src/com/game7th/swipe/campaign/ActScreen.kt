package com.game7th.swipe.campaign

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.math.Rectangle
import com.game7th.metagame.PersistentStorage
import com.game7th.metagame.account.AccountService
import com.game7th.metagame.campaign.*
import com.game7th.metagame.campaign.dto.ActConfig
import com.game7th.metagame.campaign.dto.CampaignNodeType
import com.game7th.metagame.campaign.dto.LocationConfig
import com.game7th.metagame.dto.ActProgressState
import com.game7th.metagame.dto.LocationProgressState
import com.game7th.swipe.BaseScreen
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.SwipeGameGdx
import com.game7th.swipe.TutorialKeys
import com.game7th.swipe.alchemy.AlchemyPanel
import com.game7th.swipe.alchemy.AlchemyPanelMode
import com.game7th.swipe.campaign.bottom_menu.BottomMenu
import com.game7th.swipe.campaign.inventory.ItemView
import com.game7th.swipe.campaign.party.PartyView
import com.game7th.swipe.campaign.prepare.BattlePrepareDialog
import com.game7th.swipe.campaign.top_menu.CurrencyPanel
import com.game7th.swipe.dialog.DismissStrategy
import com.game7th.swipe.forge.ForgePanel
import com.game7th.swipe.shop.ShopPanel
import com.game7th.swipe.util.animateHideToBottom
import com.game7th.swipe.util.animateShowFromBottom
import com.game7th.swipe.util.bounds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ktx.async.KtxAsync
import kotlin.math.*

sealed class UiState {
    object Hidden : UiState()
    object PartyUi : UiState()
    object ForgeUi : UiState()
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
        private val actId: Int,
        context: GdxGameContext,
        private val storage: PersistentStorage
) : BaseScreen(context, game) {

    private val batch = SpriteBatch()
    private lateinit var actConfig: ActConfig
    private lateinit var backgroundTexture: Texture

    private lateinit var config: ActConfig

    private var circleScale: Float = 0f
    private var circleOffset: Float = 0f
    private var starScale: Float = 0f
    private var starOffset: Float = 0f
    private val starAlphaStep = Math.toRadians(30.0).toFloat()
    private var lockScale: Float = 0f
    private var lockOffset: Float = 0f

    private val linkRenderer = ShapeRenderer()
    private val linkMainColor = Color(0.1f, 0.1f, 0.1f, 0.8f)
    private val linkSubColor = Color(0.1f, 0.1f, 0.1f, 0.5f)

    private val atlas = TextureAtlas(Gdx.files.internal("metagame"))

    private var mapBottomOffset = 0f
    private var scroll = 0f
    private var scrollImpulse = 0f
    lateinit var gestureDetector: GestureDetector

    lateinit var bottomMenu: BottomMenu
    lateinit var currencyView: CurrencyPanel

    private var uiState: UiState = UiState.Hidden

    private val locationCache = mutableMapOf<Int, LocationProgressState>()

    private var battlePrepareDialog: BattlePrepareDialog? = null
    private var partyUi: PartyView? = null
    private var forgeUi: ForgePanel? = null
    private var shopUi: ShopPanel? = null
    private var alchUi: AlchemyPanel? = null

    var isScrollEnabled = true

    lateinit var backgroundMusic: Music

    private var battlePreparationTutorialHook = false

    override fun show() {
        super.show()
        config = actsService.getActConfig(actId)
        val progressState: ActProgressState = actsService.getActProgress(actId)

        updateLocationProgressCache(progressState)
        actConfig = actsService.getActConfig(actId)
        backgroundTexture = Texture(Gdx.files.internal(actConfig.texture))

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
                    if (rect.contains(wx, wy) && locationCache.containsKey(node.id)) {
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

        val greyStarTexture = atlas.findRegion("star_grey")
        starScale = game.width / 24 / greyStarTexture.regionWidth
        starOffset = game.width / 48

        val lockTexture = atlas.findRegion("lock")
        lockScale = game.width / 10 / lockTexture.regionWidth
        lockOffset = game.width / 20

        bottomMenu = BottomMenu(context).apply {
            onPartyButtonPressed = this@ActScreen::onPartyButtonPressed
            onForgeButtonPressed = this@ActScreen::onForgeButtonPressed
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

        if (actId == 0 && TutorialKeys.tutorialsEnabled && storage.get(TutorialKeys.ACT1_INTRO_SHOWN)?.toBoolean() != true) {
            showIntro()
        }

        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("sb_indreams.ogg")).apply {
            volume = 0.5f
            isLooping = true
            play()
        }

        if (actId == 0 && locationCache.size == 2 && locationCache[0]?.stars == 1 && storage.get(TutorialKeys.ACT1_AFTER_FIRST_LEVEL_DONE)?.toBoolean() != true) {
            showPartyTutorialScenario()
        }
    }

    private fun updateLocationProgressCache(progressState: ActProgressState) {
        locationCache.clear()
        locationCache.putAll(progressState.asMap())
    }

    private fun normalizeScroll() {
        scroll = max(0f, min(scroll, game.scale * backgroundTexture.height - game.height + mapBottomOffset))
    }

    private fun onPartyButtonPressed() {
        if (uiState == UiState.PartyUi) return
        transiteUiState(UiState.PartyUi)
    }

    private fun onForgeButtonPressed() {
        if (uiState == UiState.ForgeUi) return
        transiteUiState(UiState.ForgeUi)
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
        if (scrollImpulse != 0f) {
            scroll += scrollImpulse * delta
            scrollImpulse *= 1 - 2f * delta
            normalizeScroll()
            if (abs(scrollImpulse) < 5f) {
                scrollImpulse = 0f
            }
        }

        batch.begin()
        batch.draw(backgroundTexture, 0f, -scroll + mapBottomOffset, backgroundTexture.width * game.scale, backgroundTexture.height * game.scale)
        batch.end()

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        linkRenderer.begin(ShapeRenderer.ShapeType.Filled)
        config.nodes.forEach {
            it.drawLinks()
        }
        linkRenderer.end()
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin()
        config.nodes.forEach {
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
        backgroundTexture.dispose()
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

        if (type != CampaignNodeType.FARM) {
            val greyStarTexture = atlas.findRegion("star_grey")
            val yellowStarTexture = atlas.findRegion("star_yellow")
            val stars = if (locationCache.containsKey(id)) locationCache[id]?.stars ?: 0 else 0
            (4 downTo 0).forEach { i ->
                val alpha = 2 * starAlphaStep - i * starAlphaStep
                val texture = if (i <= stars - 1) yellowStarTexture else greyStarTexture
                batch.draw(texture,
                        game.scale * x - circleOffset * sin(alpha) - starOffset,
                        game.scale * y - circleOffset * cos(alpha) - starOffset - scroll + mapBottomOffset,
                        0f,
                        0f,
                        texture.regionWidth.toFloat(),
                        texture.regionHeight.toFloat(),
                        starScale,
                        starScale,
                        0f)
            }
        }

        if (!locationCache.containsKey(id)) {
            val texture = atlas.findRegion("lock")
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
    }

    private fun ActProgressState.asMap(): Map<Int, LocationProgressState> {
        val map = mutableMapOf<Int, LocationProgressState>()
        locations.forEach {
            map[it.id] = it
        }
        return map
    }

    private fun getTextureForCircle(type: CampaignNodeType) = when (type) {
        CampaignNodeType.BOSS -> atlas.findRegion("circle_red")
        CampaignNodeType.FARM -> atlas.findRegion("circle_orange")
        CampaignNodeType.REGULAR -> atlas.findRegion("circle_blue")
    }

    private fun transiteUiState(state: UiState) {
        //First of all, hide previous
        when (this.uiState) {
            is UiState.PartyUi -> hidePartyUi()
            is UiState.BattlePreparation -> hideBattlePreparation()
            is UiState.ForgeUi -> hideForgeUi()
            is UiState.ShopUi -> hideShopUi()
            is UiState.AlchUi -> hideAlchUi()
        }

        uiState = state

        //Now, show stuff
        when (state) {
            is UiState.BattlePreparation -> showBattlePreparation(state.node)
            is UiState.PartyUi -> showPartyUi()
            is UiState.ForgeUi -> showForgeUi()
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

    private fun hideForgeUi() {
        forgeUi?.animateHideToBottom(context, ForgePanel.h).also { forgeUi = null }
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
        shopUi = ShopPanel(context, game.shopService, currencyView::refreshBalance).apply {
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

    private fun showForgeUi() {
        forgeUi = ForgePanel(context, this@ActScreen, game.gearService, game.accountService).apply {
            y = context.scale * 48f
        }
        stage.addActor(forgeUi)
        forgeUi?.zIndex = 0
        forgeUi?.animateShowFromBottom(context, ForgePanel.h)
    }

    private fun showBattlePreparation(node: LocationConfig) {
        battlePrepareDialog = BattlePrepareDialog(
                game = game,
                context = context,
                actId = this@ActScreen.actId,
                locationId = node.id,
                config = node,
                actsService = actsService,
                shownCallback = this@ActScreen::processBattlePrepareDialogShown) {}
        stage.addActor(battlePrepareDialog)
        battlePrepareDialog?.zIndex = max(0, stage.actors.size - 3)
    }

    private fun hideBattlePreparation() {
        battlePrepareDialog?.let { dialog ->
            dialog.remove()
            battlePrepareDialog = null
        }
    }

    private fun showIntro() {
        showDialog("vp_personage_gladiator", "Antoxa", game.context.texts["ttr_intro_1"]!!) {
            showDialog("vp_strange_figure", "Strange Figure", game.context.texts["ttr_intro_2"]!!) {
                showDialog("vp_personage_gladiator", "Antoxa", game.context.texts["ttr_intro_3"]!!) {
                    showDialog("vp_strange_figure", "Strange Figure", game.context.texts["ttr_intro_4"]!!) {
                        showDialog("vp_personage_gladiator", "Antoxa", game.context.texts["ttr_intro_5"]!!) {
                            showDialog("vp_strange_figure", "Strange Figure", game.context.texts["ttr_intro_6"]!!) {
                                val rect = config.nodes[0].let { node ->
                                    val txt = getTextureForCircle(CampaignNodeType.REGULAR)
                                    Rectangle(node.x * game.scale - circleOffset - 5f * context.scale,
                                            node.y * game.scale - circleOffset - scroll + mapBottomOffset - 5f * context.scale,
                                            txt.regionWidth * circleScale + 10f * context.scale,
                                            txt.regionHeight * circleScale + 10f * context.scale
                                    )
                                }
                                battlePreparationTutorialHook = true
                                showFocusView(game.context.texts["ttr_intro_7"]!!, rect, DismissStrategy.DISMISS_ON_INSIDE) {
                                    showBattlePreparation(config.nodes[0])
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showPartyTutorialScenario() {
        showFocusView("ttr_party_1", bottomMenu.buttonSquads.bounds(), DismissStrategy.DISMISS_ON_INSIDE) {
            transiteUiState(UiState.PartyUi)
            KtxAsync.launch {
                delay(300)
                partyUi?.let { party ->
                    showFocusView("ttr_party_2", party.personagesView.getChild(0).bounds(), DismissStrategy.DISMISS_ON_INSIDE) {
                        party.personagesView.selectionCallback?.invoke(0)
                        KtxAsync.launch {
                            delay(300)
                            party.detailView?.let { details ->
                                showFocusView("ttr_party_3", details.experienceBar.bounds(), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                    showFocusView("ttr_party_4", details.attrsBg.bounds(), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                        showFocusView("ttr_party_5", details.bodyLabel.bounds(), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                            showFocusView("ttr_party_6", details.secondAttrsBody.bounds(), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                                showFocusView("ttr_party_7", details.spiritLabel.bounds(), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                                    showFocusView("ttr_party_8", details.secondAttrsSpirit.bounds(), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                                        showFocusView("ttr_party_9", details.mindLabel.bounds(), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                                            showFocusView("ttr_party_10", details.secondAttrsMind.bounds(), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                                                showFocusView("ttr_party_11", details.buttonGear.bounds(), DismissStrategy.DISMISS_ON_INSIDE) {
                                                                    details.processGearButton()
                                                                    KtxAsync.launch {
                                                                        delay(300)
                                                                        details.inventoryView?.let { inventory ->
                                                                            showFocusView("ttr_party_12", inventory.panelScroller.bounds(), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                                                                showFocusView("ttr_party_13", inventory.panelItems.getChild(0).bounds(), DismissStrategy.DISMISS_ON_INSIDE) {
                                                                                    (inventory.panelItems.getChild(0) as? ItemView)?.let { itemView ->
                                                                                        inventory.processInventoryItemClick(game.gearService.listInventory()[0], itemView)
                                                                                        KtxAsync.launch {
                                                                                            delay(50)
                                                                                            inventory.detailPanel?.let { detailPanel ->
                                                                                                showFocusView("ttr_party_14", detailPanel.itemView.bg.bounds(), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                                                                                    showFocusView("ttr_party_15", detailPanel.affixText.bounds(), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                                                                                        showFocusView("ttr_party_16", detailPanel.actionGroup.getChild(0).bounds(), DismissStrategy.DISMISS_ON_INSIDE) {
                                                                                                            storage.put(TutorialKeys.ACT1_AFTER_FIRST_LEVEL_DONE, "true")
                                                                                                            inventory.equipFromDetailPanel(0, null)
                                                                                                            showFocusView("ttr_party_17", inventory.equippedGroup.bounds(), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                                                                                                showFocusView("ttr_party_18", details.attrsBg.bounds(), DismissStrategy.DISMISS_ON_OUTSIDE) {
                                                                                                                    dismissFocusView()
                                                                                                                    isScrollEnabled = true
                                                                                                                }
                                                                                                            }
                                                                                                        }
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun processBattlePrepareDialogShown(dialog: BattlePrepareDialog) {
        if (battlePreparationTutorialHook) {
            dismissFocusView()
            showFocusView("ttr_intro_8", dialog.getPersonageRowBounds(), DismissStrategy.DISMISS_ON_OUTSIDE) {
                showFocusView("ttr_intro_9", dialog.getEnemyRowBounds(), DismissStrategy.DISMISS_ON_OUTSIDE) {
                    showFocusView("ttr_intro_10", dialog.getDifficultyBounds(), DismissStrategy.DISMISS_ON_OUTSIDE) {
                        showFocusView("ttr_intro_11", dialog.getStartButtonBounds(), DismissStrategy.DISMISS_ON_INSIDE) {
                            dismissFocusView()
                            battlePrepareDialog?.startBattle()
                            storage.put(TutorialKeys.ACT1_INTRO_SHOWN, true.toString())
                        }
                    }
                }
            }
        }
    }


    override fun showFocusView(text: String, rect: Rectangle, strategy: DismissStrategy, dismissCallback: (() -> Unit)?) {
        isScrollEnabled = false
        super.showFocusView(text, rect, strategy, dismissCallback)
    }
}