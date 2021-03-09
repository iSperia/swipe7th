package com.game7th.swipe.campaign

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.GL20.*
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.game7th.metagame.PersistentStorage
import com.game7th.metagame.campaign.*
import com.game7th.metagame.state.ActProgressState
import com.game7th.metagame.state.LocationProgressState
import com.game7th.swipe.ScreenContext
import com.game7th.swipe.SwipeGameGdx
import com.game7th.swipe.campaign.bottom_menu.BottomMenu
import com.game7th.swipe.campaign.party.PartyView
import com.game7th.swipe.campaign.prepare.BattlePrepareDialog
import com.game7th.swipe.dialog.DialogView
import com.game7th.swipe.dialog.FocusView
import kotlin.math.*

sealed class UiState {
    object Hidden : UiState()
    object PartyUi : UiState()
    data class BattlePreparation(
            val node: LocationConfig
    ) : UiState()
}

/**
 * The screen for single campaign
 */
@Suppress("NAME_SHADOWING")
class ActScreen(
        private val game: SwipeGameGdx,
        private val actsService: ActsService,
        private val actId: Int,
        private val context: ScreenContext,
        private val storage: PersistentStorage
) : Screen {

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

    lateinit var stage: Stage
    lateinit var bottomMenu: BottomMenu

    private var uiState: UiState = UiState.Hidden

    private val locationCache = mutableMapOf<Int, LocationProgressState>()

    private var battlePrepareDialog: BattlePrepareDialog? = null
    private var partyUi: PartyView? = null

    private var isScrollEnabled = true
    private var focusView: FocusView? = null
    private var isShowingFocus1 = false
    private var isShowingFocus2 = false

    override fun show() {
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
                if (isShowingFocus2) {
                    isShowingFocus2 = false
                    dismissFocusView()
                    battlePrepareDialog?.startBattle()
                    storage.put(KEY_ACT1_INTRO_SHOWN, true.toString())
                } else {
                    closeOverlay()
                }
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
        game.multiplexer.addProcessor(0, gestureDetector)

        val texture = getTextureForCircle(CampaignNodeType.FARM)
        circleScale = game.width / 8 / texture.regionWidth
        circleOffset = game.width / 16

        val greyStarTexture = atlas.findRegion("star_grey")
        starScale = game.width / 24 / greyStarTexture.regionWidth
        starOffset = game.width / 48

        val lockTexture = atlas.findRegion("lock")
        lockScale = game.width / 10 / lockTexture.regionWidth
        lockOffset = game.width / 20

        stage = Stage(ScreenViewport())
        game.multiplexer.addProcessor(0, stage)

        bottomMenu = BottomMenu(context).apply {
            onPartyButtonPressed = this@ActScreen::onPartyButtonPressed
        }
        stage.addActor(bottomMenu)
        bottomMenu.zIndex = 100
        mapBottomOffset = context.scale * 48f

        if (actId == 0 && storage.get(KEY_ACT1_INTRO_SHOWN)?.toBoolean() != true) {
            showIntro()
        }
    }

    private fun processBattlePrepareDialogShown(dialog: BattlePrepareDialog) {
        if (isShowingFocus1) {
            isShowingFocus1 = false
            dismissFocusView()

            val coords = dialog.getStartButtonBounds()
            isShowingFocus2 = true
            showFocusView(game.context.texts["ttr_intro_8"]!!, coords)
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

        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun hide() {
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
        }

        uiState = state

        //Now, show stuff
        when (state) {
            is UiState.BattlePreparation -> showBattlePreparation(state.node)
            is UiState.PartyUi -> showPartyUi()
        }
    }

    private fun hidePartyUi() {
        partyUi?.animateHide()
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
                                isShowingFocus1 = true
                                showFocusView(game.context.texts["ttr_intro_7"]!!, rect)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun dismissFocusView() {
        focusView?.let {
            it.remove()
        }
        focusView = null
    }

    private fun showFocusView(text: String, rect: Rectangle) {
        isScrollEnabled = false
        focusView = FocusView(context, rect, text)
        stage.addActor(focusView)
    }

    private fun showDialog(portrait: String, name: String, text: String, dismisser: () -> Unit) {
        DialogView(context, name, text, portrait, dismisser).let { dialog -> stage.addActor(dialog) }
    }

    companion object {
        const val KEY_ACT1_INTRO_SHOWN = "ttr.act1.intro"
    }
}