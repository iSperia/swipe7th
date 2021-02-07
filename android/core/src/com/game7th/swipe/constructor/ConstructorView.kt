package com.game7th.swipe.constructor

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.game7th.battle.PersonageConfig
import com.game7th.swipe.GdxGameContext
import ktx.actors.*

enum class ConstructorMode {
    PERSONAGES, NPCS
}

class ConstructorView(private val context: GdxGameContext) : Group() {

    val personages = Group()
    val npcs = Group()

    val mode = ConstructorMode.PERSONAGES

    val personageCodenames = listOf("gladiator")
    val npcCodenames = listOf("slime")

    val personagesLabel = Label("Personages", Label.LabelStyle(context.font, Color.WHITE)).apply {
        width = 160f
        height = 40f
        setAlignment(Align.center)
        x = 0f
        y = 680f
        setFontScale(2f)
        color = Color.RED
    }
    val npcsLabel = Label("NPCS", Label.LabelStyle(context.font, Color.WHITE)).apply {
        width = 160f
        height = 40f
        setAlignment(Align.center)
        x = 160f
        y = 680f
        setFontScale(2f)
    }

    init {
        addActor(personagesLabel)
        addActor(npcsLabel)
        addActor(personages)
        addActor(npcs)

        val personagesBg = Image(context.atlas.createPatch("ui_dialog")).apply {
            width = 480f
            height = 680f
        }
        personagesLabel.onClick {
            showPersonages()
        }
        npcsLabel.onClick {
            showNpcs()
        }

        val npcsBg = Image(context.atlas.createPatch("ui_dialog")).apply {
            width = 480f
            height = 680f
        }

        val createButton = Label("New", Label.LabelStyle(context.font, Color.BLUE)).apply {
            width = 120f
            height = 30f
            setFontScale(2f)
            x = 180f
            y = 10f
            setAlignment(Align.center)
        }
        addActor(createButton)

        personages += personagesBg
        npcs += npcsBg

        showPersonages()

        for (i in 0..2) {
            val p = PersonagePreview(context, PersonageConfig(if (i == 1) "gladiator" else "dead", 1))
            p.x = 40f
            p.y = 720f - 80f - 120f*(i+1)
            personages += p
        }

        for (i in 0..2) {
            val n = PersonagePreview(context, PersonageConfig("slime", 1))
            n.x = 40f
            n.y = 720f - 80f - 120f*(i+1)
            npcs += n
        }
    }

    private fun showPersonages() {
        personages.isVisible = true
        npcs.isVisible = false
        personagesLabel.color = Color.RED
        npcsLabel.color = Color.WHITE
    }

    private fun showNpcs() {
        npcs.isVisible = true
        personages.isVisible = false
        personagesLabel.color = Color.WHITE
        npcsLabel.color = Color.RED
    }
}