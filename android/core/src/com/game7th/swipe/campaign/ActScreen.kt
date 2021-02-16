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
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.game7th.metagame.campaign.*
import com.game7th.metagame.state.ActProgressState
import com.game7th.metagame.state.LocationProgressState
import com.game7th.swipe.ScreenContext
import com.game7th.swipe.SwipeGameGdx
import com.game7th.swipe.campaign.prepare.BattlePrepareDialog
import kotlin.math.*

/**
 * The screen for single campaign
 */
class ActScreen(
        private val game: SwipeGameGdx,
        private val actsService: ActsService,
        private val actId: Int,
        private val context: ScreenContext
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

    private var scroll = 0f
    private var scrollImpulse = 0f
    lateinit var gestureDetector: GestureDetector

    lateinit var stage: Stage
    var showingOverlay: Boolean = false

    private val locationCache = mutableMapOf<Int, LocationProgressState>()

    override fun show() {
        config = actsService.getActConfig(actId)
        val progressState: ActProgressState = actsService.getActProgress(actId)

        updateLocationProgressCache(progressState)
        actConfig = actsService.getActConfig(actId)
        backgroundTexture = Texture(Gdx.files.internal(actConfig.texture))

        gestureDetector = GestureDetector(game.width / 12f, 0.4f, 1.1f, Float.MAX_VALUE, object : GestureDetector.GestureAdapter() {
            override fun pan(x: Float, y: Float, deltaX: Float, deltaY: Float): Boolean {
                closeOverlay()
                scrollImpulse = 0f
                scroll += deltaY
                normalizeScroll()
                return super.pan(x, y, deltaX, deltaY)
            }

            override fun fling(velocityX: Float, velocityY: Float, button: Int): Boolean {
                closeOverlay()
                scrollImpulse = velocityY
                return super.fling(velocityX, velocityY, button)
            }

            override fun tap(x: Float, y: Float, count: Int, button: Int): Boolean {
                closeOverlay()
                val wy = (game.height - y + scroll) / game.scale
                val wx = x / game.scale
                actConfig.nodes.forEach { node ->
                    val rect = Rectangle(node.x - 30, node.y - 30, 60f, 60f)
                    if (rect.contains(wx, wy) && locationCache.containsKey(node.id)) {
                        stage.addActor(BattlePrepareDialog(game = game, context = context, actId = this@ActScreen.actId, locationId = node.id))
                        showingOverlay = true
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
    }

    private fun requestLocationProgressUpdate(node: LocationConfig) {
        val stars = locationCache[node.id]?.stars ?: 0
        val normalized = min(5, stars + 1)
        if (actsService.markLocationComplete(actId, node.id, normalized)) {
            node.unlock.forEach { locationId ->
                actsService.unlockLocation(actId, locationId)
            }
        }
        updateLocationProgressCache(actsService.getActProgress(actId))
    }

    private fun updateLocationProgressCache(progressState: ActProgressState) {
        locationCache.clear()
        locationCache.putAll(progressState.asMap())
    }

    private fun normalizeScroll() {
        scroll = max(0f, min(scroll, game.scale * backgroundTexture.height - game.height))
    }

    private fun closeOverlay() {
        if (showingOverlay) {
            showingOverlay = false
            stage.actors.toList().forEach {
                it.remove()
            }
        }
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
                val y1 = y * game.scale - scroll
                val y2 = targetNode.y * game.scale - scroll
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
}