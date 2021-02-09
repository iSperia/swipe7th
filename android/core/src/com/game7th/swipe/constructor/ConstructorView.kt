package com.game7th.swipe.constructor

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.game7th.battle.BattleConfig
import com.game7th.battle.PersonageConfig
import com.game7th.battle.unit.UnitType
import com.game7th.swipe.GdxGameContext
import ktx.actors.*

class ConstructorView(
        private val context: GdxGameContext,
        val launchBattleCallback: (BattleConfig) -> Unit
) : Group() {

    val personages = Group()
    val npcs = Group()

    val personageCodenames = UnitType.values().filter { it != UnitType.UNKNOWN }
    val npcCodenames = personageCodenames

    var selector: PersonageSelector? = null

    var wavesNumber = Group()

    var waves = 1

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

        val createButtonBg = Image(context.atlas.createPatch("ui_button")).apply {
            width = 120f
            height = 30f
            x = 180f
            y = 50f
        }
        addActor(createButtonBg)

        val createButton = Label("Start", Label.LabelStyle(context.font, Color.BLUE)).apply {
            width = 120f
            height = 30f
            setFontScale(2f)
            x = 180f
            y = 50f
            setAlignment(Align.center)
        }
        addActor(createButton)
        createButton.onClick {
            //collect battle config
            val personageConfigs = (1..3).map { index ->
                personages.getChild(index) as PersonagePreview
            }.map { it.config }.filter { personageCodenames.contains(it.name) }
            val npcsConfigs = (1..3).map { index ->
                npcs.getChild(index) as PersonagePreview
            }.map { it.config }.filter { npcCodenames.contains(it.name) }

            val wavesConfig = mutableListOf<List<PersonageConfig>>()
            (0 until waves).forEach { wavesConfig.add(npcsConfigs.toList()) }
            val battleConfig = BattleConfig(personageConfigs, wavesConfig)
            launchBattleCallback(battleConfig)
        }

        personages += personagesBg
        npcs += npcsBg

        showPersonages()

        for (i in 0..2) {
            val p = PersonagePreview(context, PersonageConfig(if (i == 1) UnitType.GLADIATOR else UnitType.UNKNOWN, 1)) {
                hideSelector()
                selector = PersonageSelector(personageCodenames.map { getSkin(it) }, context) { index ->
                    val p: PersonagePreview = personages.getChild(i + 1) as PersonagePreview
                    p.config = p.config.copy(name = personageCodenames[index])
                    p.applyCharImage()
                    hideSelector()
                }.apply {
                    zIndex = 30
                    x = 55f
                    y = 150f
                }
                addActor(selector)

            }
            p.x = 40f
            p.y = 720f - 80f - 120f * (i+1)
            personages += p
        }

        for (i in 0..2) {
            val n = PersonagePreview(context, PersonageConfig(UnitType.GREEN_SLIME, 1)) {
                hideSelector()
                selector = PersonageSelector(npcCodenames.map { getSkin(it) }, context) { index ->
                    val p: PersonagePreview = npcs.getChild(i + 1) as PersonagePreview
                    p.config = p.config.copy(name = npcCodenames[index])
                    p.applyCharImage()
                    hideSelector()
                }.apply {
                    zIndex = 30
                    x = 55f
                    y = 150f
                }
                addActor(selector)
            }
            n.x = 40f
            n.y = 720f - 80f - 120f * (i+1)
            npcs += n
        }

        addActor(wavesNumber)
        wavesNumber.y = 5f
        var wid = 0f
        var padding = 10f
        for (i in 1..5) {
            val label = Label("$i waves", Label.LabelStyle(context.font, Color.WHITE))
            label.x = wid + padding
            wid += label.width + padding
            wavesNumber.addActor(label)
            label.onClick {
                waves = i
                updateWaves()
            }
        }
        updateWaves()
    }

    private fun updateWaves() {
        wavesNumber.children.withIndex().forEach {
            if (it.index + 1 == waves) {
                it.value.color = Color.BLUE
            } else {
                it.value.color = Color.WHITE
            }
        }
    }

    private fun hideSelector() {
        selector?.remove().also { selector = null }
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

    companion object {
        fun getSkin(codename: UnitType): String {
            return when (codename) {
                UnitType.GLADIATOR -> "p_gladiator"
                UnitType.POISON_ARCHER -> "personage_ranger"
                UnitType.GREEN_SLIME -> "personage_slime"
                UnitType.MACHINE_GUNNER -> "personage_gunner"
                else -> "personage_dead"
            }
        }
    }
}