package com.game7th.swipe.constructor

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.game7th.battle.PersonageConfig
import com.game7th.swipe.GdxGameContext
import ktx.actors.*

class PersonagePreview(
        val context: GdxGameContext,
        var config: PersonageConfig,
        val selectSkinCallback: () -> Unit
) : Group() {

    val bg = Image(context.atlas.createPatch("ui_button")).apply {
        width = 400f
        height = 120f
    }

    var charImage: Image? = null

    val levelChooser = Group()

    val levels = listOf(1,10,20,30,50,75,100)

    init {
        addActor(bg)
        addActor(levelChooser)

        levels.withIndex().forEach {
            val level = it.value
            val label = Label("Lvl $level", Label.LabelStyle(context.font, Color.WHITE)).apply {
                x = 80f + (it.index % 5) * 50f
                y = 120f - 40f - 30f * (it.index / 5)
                width = 40f
                height = 20f
            }

            label.onClick {
                config = config.copy(level = level)
                applyLevel()
            }
            levelChooser.addActor(label)
        }

        val deleteButton = Label("Clear", Label.LabelStyle(context.font, Color.RED)).apply {
            x = 300f
            y = 20f
            width = 50f
            height = 30f
            setAlignment(Align.center)
        }
        deleteButton.onClick {
            removeActor(charImage)
            config = config.copy(codeName = "dead", level = 1)
            applyCharImage()
            applyLevel()
        }
        addActor(deleteButton)

        applyCharImage()
        applyLevel()
    }

    private fun applyLevel() {
        val index = levels.indexOf(config.level)
        levelChooser.children.withIndex().forEach {
            if (index == it.index) {
                it.value.color = Color.RED
            } else {
                it.value.color = Color.WHITE
            }
        }
    }

    fun applyCharImage() {
        charImage?.remove()

        charImage = Image(context.atlas.findRegion(ConstructorView.getSkin(config.codeName))).apply {
            setScale(0.8f)
            x = 20f
            y = 12f
        }
        addActor(charImage)

        charImage?.onClick {
            selectSkinCallback()
        }
    }
}