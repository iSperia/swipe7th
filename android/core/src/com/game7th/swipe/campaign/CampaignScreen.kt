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
import com.google.gson.Gson
import kotlin.math.*
import kotlin.random.Random

/**
 * The screen for single campaign
 */
class CampaignScreen(
        private val game: SwipeGameGdx
) : Screen {

    private val batch = SpriteBatch()
    lateinit var campaignConfig: CampaignConfig
    lateinit var backgroundTexture: Texture
    lateinit var viewModel: CampaignViewModel

    val starsCache = mutableMapOf<Int, Int>()

    var circleScale: Float = 0f
    var circleOffset: Float = 0f

    val linkRenderer = ShapeRenderer()
    val linkMainColor = Color(0.1f, 0.1f, 0.1f, 0.8f)
    val linkSubColor = Color(0.1f, 0.1f, 0.1f, 0.5f)

    val atlas = TextureAtlas(Gdx.files.internal("metagame"))

    private var scroll = 0f
    private var scrollImpulse = 0f
    lateinit var gestureDetector: GestureDetector

    override fun show() {
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
                campaignConfig.nodes.forEach {
                    val rect = Rectangle(it.x - 30, it.y - 30, 60f, 60f)
                    if (rect.contains(wx, wy)) {
                        val stars = starsCache[it.id]
                        stars?.let { stars ->
                            val normalized = if (stars > 4) 0 else stars + 1
                            starsCache[it.id] = normalized
                        }

                    }
                }
                return super.tap(x, y, count, button)
            }
        })
        game.multiplexer.addProcessor(0, gestureDetector)

        val texture = getTextureForCircle(CampaignNodeType.FARM)
        val circleScale = game.width / 8 / texture.regionWidth
        val circleOffset = game.width / 16

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
        val circleScale = game.width / 8 / texture.regionWidth
        val circleOffset = game.width / 16
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
            val stars = starsCache[id] ?: Random.nextInt(0, 6).apply { starsCache[id] = this }
            val starScale = game.width / 24 / greyStarTexture.regionWidth
            val starOffset = game.width / 48
            val alphaStep = Math.toRadians(30.0).toFloat()
            (4 downTo 0).forEach { i ->
                val alpha = 2 * alphaStep - i * alphaStep
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
    }

    private fun getTextureForCircle(type: CampaignNodeType) = when (type) {
        CampaignNodeType.BOSS -> atlas.findRegion("circle_red")
        CampaignNodeType.FARM -> atlas.findRegion("circle_orange")
        CampaignNodeType.REGULAR -> atlas.findRegion("circle_blue")
    }
}