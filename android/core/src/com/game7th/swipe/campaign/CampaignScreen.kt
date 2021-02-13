package com.game7th.swipe.campaign

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.game7th.metagame.campaign.CampaignConfig
import com.game7th.metagame.campaign.CampaignNodeConfig
import com.game7th.metagame.campaign.CampaignNodeType
import com.game7th.metagame.campaign.CampaignViewModel
import com.game7th.swipe.SwipeGameGdx
import com.google.gson.Gson

/**
 * The screen for single campaign
 */
class CampaignScreen(
        private val game: SwipeGameGdx
) : Screen {

    private val batch = SpriteBatch()
    private val background: Sprite
    val campaignConfig: CampaignConfig
    val viewModel: CampaignViewModel

    val linkRenderer = ShapeRenderer().apply {
        setColor(Color.BLACK)
    }

    val atlas = TextureAtlas(Gdx.files.internal("metagame"))

    init {
        val campaignFile = Gdx.files.internal("campaign_0.json")
        campaignConfig = Gson().fromJson<CampaignConfig>(campaignFile.readString(), CampaignConfig::class.java)
        viewModel = CampaignViewModel(campaignConfig)
        background = Sprite(Texture(Gdx.files.internal(viewModel.texture))).apply {
            setScale(game.scale)
            setCenter(0f, 0f)
            translate(game.width / 2f, game.height /2f)
        }
    }

    override fun show() {

    }

    override fun render(delta: Float) {
        batch.begin()
        background.draw(batch)
        batch.end()

        linkRenderer.begin(ShapeRenderer.ShapeType.Filled)
        viewModel.nodeConfigs.forEach {
            it.drawLinks()
        }
        linkRenderer.end()

        batch.begin()
        viewModel.nodeConfigs.forEach {
            it.draw(batch)
        }
        batch.end()
    }

    override fun resize(width: Int, height: Int) {
        TODO("Not yet implemented")
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun hide() {
        background.texture.dispose()
        batch.dispose()
    }

    override fun dispose() {
    }

    private fun CampaignNodeConfig.drawLinks() {
        unlock.forEach { index ->
            campaignConfig.findNode(index)?.let { targetNode ->
                val x1 = x * game.scale
                val x2 = targetNode.x * game.scale
                val y1 = y * game.scale
                val y2 = targetNode.y * game.scale
                linkRenderer.rectLine(x1, y1, x2, y2, game.width * 3f / 480f)
            }
        }
    }

    private fun CampaignNodeConfig.draw(batch: SpriteBatch) {
        val texture = getTextureForCircle(type)
        val scale = game.width / 8 / texture.regionWidth
        val offset = scale * texture.regionWidth / 2
        batch.draw(texture, game.scale * x - offset, game.scale * y - offset , 0f, 0f, texture.regionWidth.toFloat(), texture.regionHeight.toFloat(), scale, scale, 0f)
    }

    private fun getTextureForCircle(type: CampaignNodeType) = when (type) {
        CampaignNodeType.BOSS -> atlas.findRegion("circle_red")
        CampaignNodeType.FARM -> atlas.findRegion("circle_orange")
        CampaignNodeType.REGULAR -> atlas.findRegion("circle_blue")
    }
}