package com.game7th.swipe.game.actors.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.AlphaAction
import com.badlogic.gdx.scenes.scene2d.actions.DelayAction
import com.badlogic.gdx.scenes.scene2d.actions.ParallelAction
import com.badlogic.gdx.scenes.scene2d.actions.ScaleToAction
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.game7th.metagame.account.dto.PersonageExperienceResult
import com.game7th.metagame.account.RewardData
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.campaign.inventory.ItemView
import com.game7th.swipe.campaign.party.ExperienceBar
import com.game7th.swipe.campaign.reward.CurrencyRewardView
import ktx.actors.alpha
import kotlin.math.min

class GameFinishedDialog(
        private val context: GdxGameContext,
        private val text: String,
        private val expResult: List<PersonageExperienceResult>,
        private val rewards: List<RewardData>,
        callback: () -> Unit
        ) : Group() {

    val background = Image(context.uiAtlas.findRegion("ui_dialog")).apply {
        width = 400f * context.scale
        height = 300f * context.scale
        zIndex = 5
        addActor(this)
    }

    val buttonClose = Image(context.uiAtlas.findRegion("ui_button_simple")).apply {
        width = 120f * context.scale
        height = 20f * context.scale
        zIndex = 6
        x = 140f * context.scale
        y = 20f * context.scale
        addActor(this)

        addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                super.clicked(event, x, y)
                callback()
            }
        })
    }

    val buttonCloseLabel = Label("CLOSE", Label.LabelStyle(context.font, Color.BLACK)).apply {
        width = 120f * context.scale
        height = 20f * context.scale
        zIndex = 7
        x = 140f * context.scale
        y = 20f * context.scale
        setFontScale(0.5f * context.scale)
        setAlignment(Align.center)
        addActor(this)
        touchable = Touchable.disabled
    }

    val label = Label(text, Label.LabelStyle(context.font, Color.RED)).apply {
        setAlignment(Align.center)
        width = 400f * context.scale
        height = 25f * context.scale
        setFontScale(1f * context.scale)
        x = 0f
        y = 265f * context.scale
        zIndex = 10
        addActor(this)
    }

    val experienceBar = ExperienceBar(context, 380f * context.scale, 40f * context.scale, expResult.firstOrNull()?.oldExp ?: 0, expResult.firstOrNull()?.maxExp ?: 100, false).apply {
        x = 10f * context.scale
        y = 220f * context.scale
        touchable = Touchable.disabled
    }

    val newLevelText = Label("", Label.LabelStyle(context.font, Color.YELLOW)).apply {
        width = 380f * context.scale
        height = 30f * context.scale
        setFontScale(context.scale)
        x = 10f * context.scale
        y = 190f * context.scale
        zIndex = 14
        setAlignment(Align.left)
        addActor(this)
        isVisible = true
    }

    val statsText = Label("", Label.LabelStyle(context.font, Color.BLUE)).apply {
        width = 380f * context.scale
        height = 25f * context.scale
        setFontScale(0.75f * context.scale)
        x = 10f * context.scale
        y = 150f * context.scale
        zIndex = 15
        setAlignment(Align.left)
        addActor(this)
    }

    val rewardsRoot = Group().apply {
        x = 10f * context.scale
        y = 70f * context.scale
        zIndex = 16
        this@GameFinishedDialog.addActor(this)
        isVisible = false
    }

    var timePassed = 0f

    init {
        expResult.firstOrNull()?.let {
            addActor(experienceBar)
        }

        rewardsRoot.isVisible = true
        rewards.forEachIndexed { index, reward ->
            when (reward) {
                is RewardData.ArtifactRewardData -> {
                    val artifact = ItemView(context, reward.item, true, 70 * context.scale).apply {
                        x = 80f * index * context.scale
                        y = 0f
                        setScale(2f)
                        alpha = 0f
                    }
                    rewardsRoot.addActor(artifact)
                    artifact.addAction(DelayAction(index * 0.1f).apply { action =
                            ParallelAction(
                                    AlphaAction().apply { alpha = 1f; duration = 0.3f },
                                    ScaleToAction().apply { setScale(1f, 1f); duration = 0.3f }
                            )
                    })
                }
                is RewardData.CurrencyRewardData -> {
                    val reward = CurrencyRewardView(context, reward.currency, reward.amount, 70 * context.scale).apply {
                        x = 80f * index * context.scale
                        y = 0f
                        setScale(2f)
                        alpha = 0f
                    }
                    rewardsRoot.addActor(reward)
                    reward.addAction(DelayAction(index * 0.1f).apply { action =
                            ParallelAction(
                                    AlphaAction().apply { alpha = 1f; duration = 0.3f },
                                    ScaleToAction().apply { setScale(1f, 1f); duration = 0.3f }
                            )
                    })
                }
            }
        }
    }

    private var activeExpResultStep = -1

    override fun act(delta: Float) {
        super.act(delta)

        timePassed += delta
        var shownExpStep = min(expResult.size, (timePassed / 1.1f).toInt())
        if (shownExpStep > activeExpResultStep) {
            if (activeExpResultStep >= 0) {
                if (expResult[activeExpResultStep].levelUp) {
                    //show level up
                    newLevelText.setText("NEW LEVEL ${expResult[activeExpResultStep].newLevel}")
                    newLevelText.setScale(1.5f)
                    newLevelText.addAction(ScaleToAction().apply { setScale(1f); duration=0.25f })

                    statsText.setText(expResult[activeExpResultStep].gainedStats.toString())
                }
            }

            activeExpResultStep = shownExpStep
            if (shownExpStep < expResult.size) {
                experienceBar.animateProgress(expResult[shownExpStep].oldExp, expResult[shownExpStep].maxExp, expResult[shownExpStep].newExp)
            }
        }
    }
}