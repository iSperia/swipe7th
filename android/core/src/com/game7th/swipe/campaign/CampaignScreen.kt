package com.game7th.swipe.campaign

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.math.Rectangle
import com.game7th.metagame.campaign.CampaignConfig
import com.game7th.metagame.campaign.CampaignNodeConfig
import com.game7th.metagame.campaign.CampaignNodeType
import com.game7th.metagame.campaign.CampaignViewModel
import com.game7th.swipe.SwipeGameGdx
import com.game7th.swipe.state.CampaignState
import com.game7th.swipe.state.LocationState
import com.game7th.swipe.state.StateStorage
import com.google.gson.Gson
import kotlin.math.*
import kotlin.random.Random

/**
 * The screen for single campaign
 */
class CampaignScreen(
        private val game: SwipeGameGdx,
        private val storage: StateStorage
) : Screen {

    private val batch = SpriteBatch()
    lateinit var campaignConfig: CampaignConfig
    lateinit var backgroundTexture: Texture
    lateinit var viewModel: CampaignViewModel

    var circleScale: Float = 0f
    var circleOffset: Float = 0f
    var starScale: Float = 0f
    var starOffset: Float = 0f
    val starAlphaStep = Math.toRadians(30.0).toFloat()
    var lockScale: Float = 0f
    var lockOffset: Float = 0f

    val linkRenderer = ShapeRenderer()
    val linkMainColor = Color(0.1f, 0.1f, 0.1f, 0.8f)
    val linkSubColor = Color(0.1f, 0.1f, 0.1f, 0.5f)

    val atlas = TextureAtlas(Gdx.files.internal("metagame"))

    private var scroll = 0f
    private var scrollImpulse = 0f
    lateinit var gestureDetector: GestureDetector

    lateinit var campaign: CampaignState
    private val locationCache = mutableMapOf<Int, LocationState>()

    override fun show() {
        storage.load().let { gameState ->
            locationCache.putAll(gameState.campaigns[0].asMap())
            campaign = gameState.campaigns.first { it.id == 0 }
        }

        val campaignFile = Gdx.files.internal("campaign_0.json")
        campaignConfig = Gson().fromJson<CampaignConfig>(campaignFile.readString(), CampaignConfig::class.java)
        viewModel = CampaignViewModel(campaignConfig)
        backgroundTexture = Texture(Gdx.files.internal(campaignConfig.texture))

        gestureDetector = GestureDetector(game.width / 12f, 0.4f, 1.1f, Float.MAX_VALUE, object : GestureDetector.GestureAdapter() {
            override fun pan(x: Float, y: Float, deltaX: Float, deltaY: Float): Boolean {
                scrollImpulse = 0f
                scroll += deltaY
                normalizeScroll()
                return super.pan(x, y, deltaX, deltaY)
            }

            override fun fling(velocityX: Float, velocityY: Float, button: Int): Boolean {
                scrollImpulse = velocityY
                return super.fling(velocityX, velocityY, button)
            }

            override fun tap(x: Float, y: Float, count: Int, button: Int): Boolean {
                val wy = (game.height - y + scroll) / game.scale
                val wx = x / game.scale
                campaignConfig.nodes.forEach { node ->
                    val rect = Rectangle(node.x - 30, node.y - 30, 60f, 60f)
                    if (rect.contains(wx, wy) && locationCache.containsKey(node.id)) {
                        val stars = locationCache[node.id]?.stars ?: 0
                        val normalized = min(5, stars + 1)
                        locationCache[node.id] = LocationState(node.id, normalized)
                        //unlock linked
                        campaignConfig.nodes.first { it.id == node.id }.unlock.forEach { unlock ->
                            if (!locationCache.containsKey(unlock)) {
                                locationCache[unlock] = LocationState(unlock, 0)
                            }
                        }
                        storage.updateCampaign(0, locationCache.toState(campaign))
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
    }

    private fun normalizeScroll() {
        scroll = max(0f, min(scroll, game.scale * backgroundTexture.height - game.height))
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
        batch.draw(backgroundTexture, 0f, -scroll, backgroundTexture.width * game.scale, backgroundTexture.height * game.scale)
        batch.end()

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        linkRenderer.begin(ShapeRenderer.ShapeType.Filled)
        viewModel.nodeConfigs.forEach {
            it.drawLinks()
        }
        linkRenderer.end()
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin()
        viewModel.nodeConfigs.forEach {
            it.draw(batch)
        }
        batch.end()
    }

    override fun resize(width: Int, height: Int) {
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun hide() {
        game.multiplexer.removeProcessor(gestureDetector)
        backgroundTexture.dispose()
        batch.dispose()
    }

    override fun dispose() {
    }

    private fun CampaignNodeConfig.drawLinks() {
        unlock.forEach { index ->
            campaignConfig.findNode(index)?.let { targetNode ->
                val x1 = x * game.scale
                val x2 = targetNode.x * game.scale
                val y1 = y * game.scale - scroll
                val y2 = targetNode.y * game.scale - scroll
                linkRenderer.color = linkSubColor
                linkRenderer.rectLine(x1, y1, x2, y2, game.width * 6f / 480f)
                linkRenderer.color = linkMainColor
                linkRenderer.rectLine(x1, y1, x2, y2, game.width * 3f / 480f)
            }
        }
    }

    private fun CampaignNodeConfig.draw(batch: SpriteBatch) {
        val texture = getTextureForCircle(type)
        batch.draw(texture,
                game.scale * x - circleOffset,
                game.scale * y - circleOffset - scroll,
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
                        game.scale * y - circleOffset * cos(alpha) - starOffset - scroll,
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
                    game.scale * y - lockOffset - scroll,
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

    private fun CampaignState.asMap(): Map<Int, LocationState> {
        val map = mutableMapOf<Int, LocationState>()
        locations.forEach {
            map[it.id] = it
        }
        return map
    }

    private fun Map<Int, LocationState>.toState(state: CampaignState): CampaignState {
        return state.copy(locations = locationCache.values.toList())
    }

    private fun getTextureForCircle(type: CampaignNodeType) = when (type) {
        CampaignNodeType.BOSS -> atlas.findRegion("circle_red")
        CampaignNodeType.FARM -> atlas.findRegion("circle_orange")
        CampaignNodeType.REGULAR -> atlas.findRegion("circle_blue")
    }
}