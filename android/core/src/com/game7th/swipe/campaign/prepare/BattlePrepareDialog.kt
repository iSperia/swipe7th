package com.game7th.swipe.campaign.prepare

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.*
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.game7th.swipe.SwipeGameGdx
import ktx.actors.alpha
import ktx.actors.onClick
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.utils.Align
import com.game7th.metagame.campaign.ActsService
import com.game7th.metagame.campaign.dto.LocationConfig
import com.game7th.metagame.dto.UnitConfig
import com.game7th.metagame.dto.UnitType
import com.game7th.metagame.network.NetworkError
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.campaign.plist.PersonageScrollActor
import com.game7th.swipe.game.GameScreen
import com.game7th.swiped.api.PersonageDto
import kotlinx.coroutines.launch
import ktx.async.KtxAsync

/**
 * Dialog is shown before battle is started after player clicked some unlocked location
 */
class BattlePrepareDialog(
        private val game: SwipeGameGdx,
        private val accountId: String,
        private val context: GdxGameContext,
        private val actId: String,
        private val locationId: Int,
        private val config: LocationConfig,
        private val actsService: ActsService,
        private val shownCallback: (BattlePrepareDialog) -> Unit,
        private val dismissCallback: () -> Unit
) : Group() {

    val scale = game.scale

    val background: Image
    val buttonStart: TextButton
    val vsIcon: Image

    lateinit var scrollPersonages: ScrollPane
    lateinit var personagesGroup: PersonageScrollActor

    val npcPersonages: ScrollPane
    val npcGroup: PersonageScrollActor

    val w: Float
    val h: Float

    lateinit var personages: List<PersonageDto>

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

        background = Image(context.commonAtlas.createPatch("ui_hor_panel")).apply {
            width = this@BattlePrepareDialog.width
            height = this@BattlePrepareDialog.height
        }
        background.onClick { }
        addActor(background)

        buttonStart = TextButton("Start", TextButton.TextButtonStyle(
                NinePatchDrawable(context.commonAtlas.createPatch("ui_button_simple")),
                NinePatchDrawable(context.commonAtlas.createPatch("ui_button_pressed")),
                null,
                context.regularFont
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

        KtxAsync.launch {
            personages = game.accountService.getPersonages()
            personagesGroup = PersonageScrollActor(context, personages.map { UnitConfig(UnitType.valueOf(it.unit), it.level) }, scrollHeight, true)
            scrollPersonages = ScrollPane(personagesGroup).apply {
                x = 20f * context.scale
                width = 380f * context.scale
                y = 220f * context.scale
                height = scrollHeight
            }
            addActor(scrollPersonages)
        }


        npcGroup = PersonageScrollActor(context, config.waves.flatten(), scrollHeight, false)

        npcPersonages = ScrollPane(npcGroup).apply {
            x = 20f * context.scale
            width = 380 * context.scale
            y = 50f * context.scale
            height = scrollHeight
        }
        addActor(npcPersonages)

        vsIcon = Image(context.commonAtlas.findRegion("ui_icon_vs")).apply {
            x = 190f * game.scale
            y = 190f * game.scale
            width = 40f * game.scale
            height = 40f * game.scale
            touchable = Touchable.disabled
        }
        addActor(vsIcon)
    }

    private suspend fun showGameScreen() {
        try {
            val result = game.api.encounterLocation(actId, locationId, personages[personagesGroup.selectedIndex % personages.size].id)
            game.switchScreen(GameScreen(
                    game,
                    result,
                    accountId,
                    actId,
                    locationId,
                    personages[personagesGroup.selectedIndex % personages.size],
                    actsService,
                    game.gearService,
                    game.storage,
                    game.context
            ))
        } catch (e: NetworkError) {
            e.printStackTrace()
        }
    }

    fun startBattle() {
        dismissCallback()
        KtxAsync.launch {
            showGameScreen()
        }
    }
}