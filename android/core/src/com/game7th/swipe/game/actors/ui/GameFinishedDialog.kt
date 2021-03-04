package com.game7th.swipe.game.actors.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.ScaleToAction
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.game7th.metagame.account.PersonageExperienceResult
import com.game7th.swipe.GdxGameContext

class GameFinishedDialog(
        private val context: GdxGameContext,
        private val text: String,
        private val expResult: PersonageExperienceResult?,
        callback: () -> Unit
        ) : Group() {

    val background = Image(context.uiAtlas.findRegion("ui_dialog")).apply {
        width = 400f
        height = 300f
        zIndex = 5
        addActor(this)
    }

    val buttonClose = Image(context.uiAtlas.findRegion("ui_button_simple")).apply {
        width = 120f
        height = 20f
        zIndex = 6
        x = 140f
        y = 20f
        addActor(this)

        addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                super.clicked(event, x, y)
                callback()
            }
        })
    }

    val buttonCloseLabel = Label("CLOSE", Label.LabelStyle(context.font, Color.BLACK)).apply {
        width = 120f
        height = 20f
        zIndex = 7
        x = 140f
        y = 20f
        setFontScale(0.5f)
        setAlignment(Align.center)
        addActor(this)
        touchable = Touchable.disabled
    }

    val label = Label(text, Label.LabelStyle(context.font, Color.RED)).apply {
        setAlignment(Align.center)
        width = 400f
        height = 25f
        setFontScale(1f)
        x = 0f
        y = 265f
        zIndex = 10
        addActor(this)
    }

    val expBackground = Image(context.atlas.findRegion("bar_black")).apply {
        width = 380f
        height = 40f
        x = 10f
        y = 220f
        zIndex = 11
        addActor(this)
    }

    val expForeground = Image(context.atlas.findRegion("bar_purple")).apply {
        width = 380f
        height = 40f
        x = 10f
        y = 220f
        zIndex = 12
        addActor(this)
    }

    val expText = Label(expResult?.oldExp?.toString(), Label.LabelStyle(context.font, Color.BLUE)).apply {
        width = 380f
        height = 25f
        setFontScale(0.75f)
        x = 10f
        y = 220f
        zIndex = 13
        setAlignment(Align.left)
        addActor(this)
    }

    val newLevelText = Label("New level: ${expResult?.newLevel}!", Label.LabelStyle(context.font, Color.YELLOW)).apply {
        width = 380f
        height = 30f
        setFontScale(1f)
        x = 10f
        y = 190f
        zIndex = 14
        setAlignment(Align.left)
        addActor(this)
        isVisible = false
    }

    val statsText = Label("", Label.LabelStyle(context.font, Color.BLUE)).apply {
        width = 380f
        height = 25f
        setFontScale(0.75f)
        x = 10f
        y = 150f
        zIndex = 15
        setAlignment(Align.left)
        addActor(this)
        isVisible = false
    }

    var timePassed = 0f
    var newLevelShown = false

    init {
        if (expResult == null) {
            expBackground.isVisible = false
            expForeground.isVisible = false
            expText.isVisible = false
        } else {
            expForeground.scaleX = expResult.oldExp.toFloat() / expResult.maxExp
            expForeground.addAction(ScaleToAction().apply {
                setScale(expResult.newExp.toFloat() / expResult.maxExp, 1f)
                duration = 3f
            })
        }
    }

    override fun act(delta: Float) {
        super.act(delta)

        timePassed += delta
        if (expResult != null) {
            if (timePassed < 3f) {
                val valueToDraw = (expResult.oldExp + (expResult.newExp - expResult.oldExp) * timePassed / 3f).toInt().toString()
                expText.setText(valueToDraw)
            } else if (!newLevelShown) {
                newLevelShown = true
                expText.setText(expResult.newExp)
                if (expResult.levelUp) {
                    newLevelText.isVisible = true
                    statsText.isVisible = true
                    statsText.setText(
                            (if (expResult.gainedStats?.body ?: 0 > 0) "BODY +${expResult.gainedStats?.body} " else "") +
                                    (if (expResult.gainedStats?.spirit ?: 0 > 0) "SPIRIT +${expResult.gainedStats?.spirit} " else "") +
                                    (if (expResult.gainedStats?.mind ?: 0 > 0) "MIND +${expResult.gainedStats?.mind} " else "")
                    )
                }
            }
        }
    }
}