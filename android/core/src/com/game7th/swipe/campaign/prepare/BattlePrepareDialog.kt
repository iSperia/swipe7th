package com.game7th.swipe.campaign.prepare

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.game7th.swipe.SwipeGameGdx
import ktx.actors.alpha
import ktx.actors.onClick
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.actions.AlphaAction
import com.badlogic.gdx.scenes.scene2d.actions.ParallelAction
import com.badlogic.gdx.scenes.scene2d.actions.ScaleToAction
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.game7th.metagame.campaign.ActsService
import com.game7th.metagame.campaign.LocationConfig
import com.game7th.metagame.unit.SquadConfig
import com.game7th.metagame.unit.UnitConfig
import com.game7th.swipe.ScreenContext
import com.game7th.swipe.game.GameScreen

/**
 * Dialog is shown before battle is started after player clicked some unlocked location
 */
class BattlePrepareDialog(
        private val game: SwipeGameGdx,
        private val context: ScreenContext,
        private val actId: Int,
        private val locationId: Int,
        private val config: LocationConfig,
        private val actsService: ActsService
) : Group() {

    val scale = game.scale

    val starImages = Group()

    val background: Image
    val labelDifficulty: Label
    val buttonStart: TextButton
    val personageSquadBrowser: SquadBrowserActor
    val npcSquadBrowser: SquadBrowserActor

    val w: Float
    val h: Float

    var difficulty = 1

    private val personages = game.accountService.getPersonages()

    val personageAdapter = object : SquadBrowserAdapter {
        override fun count() = personages.size

        override fun getSquad(index: Int): SquadConfig {
            return SquadConfig(personages[index].unit.toString(), listOf(
                    UnitConfig(personages[index].unit, personages[index].level)
            ))
        }
    }

    val npcAdapter = object : SquadBrowserAdapter {
        override fun count() = config.waves.size

        override fun getSquad(index: Int): SquadConfig {
            val bonus = (difficulty - 1) * 3
            return SquadConfig("Wave ${index + 1}", config.waves[index].map {
                it.copy(level = it.level + bonus)
            })
        }
    }

    init {
        w = scale * 336
        h = scale * 330f
        width = w
        height = h
        x = (game.width - this.width) / 2f
        y = (game.height - this.height) / 2f
        setOrigin(Align.center)
        alpha = 0f
        setScale(0f)

        addAction(ParallelAction(
                AlphaAction().apply { alpha = 1f; duration = 0.3f },
                ScaleToAction().apply { setScale(1f); duration = 0.3f }
        ))

        background = Image(context.uiAtlas.createPatch("ui_dialog_bg.fw")).apply {
            width = this@BattlePrepareDialog.width + 28
            height = this@BattlePrepareDialog.height + 28
            x = -14f
            y = -14f
        }
        background.onClick { }
        addActor(background)

        labelDifficulty = Label("Chose difficulty:", Label.LabelStyle(context.font, Color.WHITE)).apply {
            x = 94 * game.scale
            y = 300f * game.scale
            width = 71 * game.scale
            height = 30 * game.scale
            setFontScale(1f)
        }
        labelDifficulty.touchable = Touchable.disabled
        addActor(labelDifficulty)

        addActor(starImages)
        (1..5).forEach { i ->
            val image = Image(context.uiAtlas.findRegion("star_grey")).apply {
                width = 24 * game.scale
                height = 24 * game.scale
                x = (168 + i * 26) * game.scale
                y = 303f * game.scale
            }
            starImages.addActor(image)
            image.onClick {
                changeDifficulty(i)
            }

        }
        applyDifficulty()

        buttonStart = TextButton("Start", TextButton.TextButtonStyle(
                TextureRegionDrawable(context.uiAtlas.findRegion("ui_button.fw")),
                TextureRegionDrawable(context.uiAtlas.findRegion("ui_button_dn.fw")),
                null,
                context.font
        )).apply {
            label.setFontScale(1f)
            x = 240 * game.scale
            y = 5f * game.scale
            width = game.scale * 90
            height = 20 * game.scale
        }
        addActor(buttonStart)
        buttonStart.onClick {
            showGameScreen()
        }

        personageSquadBrowser = SquadBrowserActor(context, personageAdapter).apply {
            x = 6 * context.scale
            y = 30 * context.scale
        }
        addActor(personageSquadBrowser)

        npcSquadBrowser = SquadBrowserActor(context, npcAdapter).apply {
            x = 170 * context.scale
            y = 30 * context.scale
        }
        addActor(npcSquadBrowser)
    }

    private fun showGameScreen() {
        game.switchScreen(GameScreen(
                game,
                actId,
                locationId,
                difficulty,
                personages[personageSquadBrowser.index],
                actsService
        ))
    }

    private fun changeDifficulty(i: Int) {
        difficulty = i
        applyDifficulty()
        npcSquadBrowser.update()
    }

    private fun applyDifficulty() {
        starImages.children.withIndex().forEach { (index, star) ->
            if (index < difficulty) {
                (star as Image).drawable = TextureRegionDrawable(context.uiAtlas.findRegion("star_yellow"))
            } else {
                (star as Image).drawable = TextureRegionDrawable(context.uiAtlas.findRegion("star_grey"))
            }
        }
    }
}