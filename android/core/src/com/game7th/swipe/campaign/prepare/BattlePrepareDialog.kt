package com.game7th.swipe.campaign.prepare

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.*
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.game7th.swipe.SwipeGameGdx
import ktx.actors.alpha
import ktx.actors.onClick
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.game7th.metagame.campaign.ActsService
import com.game7th.metagame.campaign.LocationConfig
import com.game7th.metagame.unit.UnitConfig
import com.game7th.swipe.ScreenContext
import com.game7th.swipe.campaign.plist.PersonageScrollActor
import com.game7th.swipe.game.GameScreen
import org.w3c.dom.css.Rect

/**
 * Dialog is shown before battle is started after player clicked some unlocked location
 */
class BattlePrepareDialog(
        private val game: SwipeGameGdx,
        private val context: ScreenContext,
        private val actId: Int,
        private val locationId: Int,
        private val config: LocationConfig,
        private val actsService: ActsService,
        private val shownCallback: (BattlePrepareDialog) -> Unit,
        private val dismissCallback: () -> Unit
) : Group() {

    val scale = game.scale

    val starImages = Group()

    val background: Image
    val labelDifficulty: Label
    val buttonStart: TextButton
    val vsIcon: Image

    val scrollPersonages: ScrollPane
    val personagesGroup: PersonageScrollActor

    val npcPersonages: ScrollPane
    val npcGroup: PersonageScrollActor

    val w: Float
    val h: Float

    var difficulty = 1

    private val personages = game.accountService.getPersonages()

    init {
        w = scale * 420f
        h = scale * 420f
        width = w
        height = h
        x = (game.width - this.width) / 2f
        y = (game.height - this.height) / 2f
        setOrigin(Align.center)
        alpha = 0f
        setScale(0f)

        addAction(
                SequenceAction(
                        ParallelAction(
                                AlphaAction().apply { alpha = 1f; duration = 0.3f },
                                ScaleToAction().apply { setScale(1f); duration = 0.3f }
                        ), RunnableAction().apply { setRunnable { shownCallback(this@BattlePrepareDialog) } }))

        background = Image(context.uiAtlas.findRegion("ui_dialog")).apply {
            width = this@BattlePrepareDialog.width
            height = this@BattlePrepareDialog.height
        }
        background.onClick { }
        addActor(background)

        buttonStart = TextButton("Start", TextButton.TextButtonStyle(
                TextureRegionDrawable(context.uiAtlas.findRegion("ui_button_simple")),
                TextureRegionDrawable(context.uiAtlas.findRegion("ui_button_pressed")),
                null,
                context.font
        )).apply {
            label.setFontScale(1f)
            label.color = Color.BLACK
            x = 165 * game.scale
            y = 10f * game.scale
            width = game.scale * 90
            height = 20 * game.scale
        }
        addActor(buttonStart)
        buttonStart.onClick {
           startBattle()
        }

        val scrollHeight = 150f * context.scale

        personagesGroup = PersonageScrollActor(context, personages.map { UnitConfig(it.unit, it.level) }, scrollHeight, true)
        scrollPersonages = ScrollPane(personagesGroup).apply {
            x = 20f * context.scale
            width = 380f * context.scale
            y = 220f * context.scale
            height = scrollHeight
        }
        addActor(scrollPersonages)

        npcGroup = PersonageScrollActor(context, config.waves.flatMap { it }.map { it }, scrollHeight, false)
        npcPersonages = ScrollPane(npcGroup).apply {
            x = 20f * context.scale
            width = 380 * context.scale
            y = 50f * context.scale
            height = scrollHeight
        }
        addActor(npcPersonages)

        vsIcon = Image(context.uiAtlas.findRegion("ui_icon_vs")).apply {
            x = 190f * game.scale
            y = 190f * game.scale
            width = 40f * game.scale
            height = 40f * game.scale
            touchable = Touchable.disabled
        }
        addActor(vsIcon)

        labelDifficulty = Label("Select difficulty:", Label.LabelStyle(context.font, Color.BLACK)).apply {
            x = 0f
            y = 380f * game.scale
            width = 210f * game.scale
            height = 30f * game.scale
            setFontScale(1f)
            setAlignment(Align.right)
        }
        labelDifficulty.touchable = Touchable.disabled
        addActor(labelDifficulty)

        addActor(starImages)
        (1..5).forEach { i ->
            val image = Image(context.uiAtlas.findRegion("star_grey")).apply {
                width = 30f * game.scale
                height = 30f * game.scale
                x = (230 + 35f * (i - 1)) * game.scale
                y = 380f * game.scale
            }
            starImages.addActor(image)
            image.onClick {
                changeDifficulty(i)
            }

        }
        applyDifficulty()
    }

    private fun showGameScreen() {
        game.switchScreen(GameScreen(
                game,
                actId,
                locationId,
                difficulty,
                personages[personagesGroup.selectedIndex % personages.size],
                actsService
        ))
    }

    private fun changeDifficulty(i: Int) {
        difficulty = i
        applyDifficulty()
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

    fun getPersonageRowBounds(): Rectangle {
        val coords = localToStageCoordinates(Vector2(scrollPersonages.x, scrollPersonages.y))
        return Rectangle(coords.x, coords.y, scrollPersonages.width, scrollPersonages.height)
    }

    fun getEnemyRowBounds(): Rectangle {
        val coords = localToStageCoordinates(Vector2(npcPersonages.x, npcPersonages.y))
        return Rectangle(coords.x, coords.y, npcPersonages.width, npcPersonages.height)
    }

    fun getDifficultyBounds(): Rectangle {
        val coords = localToStageCoordinates(Vector2(labelDifficulty.x, labelDifficulty.y))
        return Rectangle(coords.x, coords.y, context.scale * 420f, 30f * context.scale)
    }

    fun getStartButtonBounds(): Rectangle {
        val coords = localToStageCoordinates(Vector2(buttonStart.x, buttonStart.y))
        return Rectangle(coords.x, coords.y, buttonStart.width, buttonStart.height)
    }

    fun startBattle() {
        dismissCallback()
        showGameScreen()
    }
}