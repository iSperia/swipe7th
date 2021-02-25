package com.game7th.swipe.game.battle.hud

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.game7th.battle.personage.PersonageViewModel
import com.game7th.swipe.GdxGameContext
import ktx.actors.alpha

class PersonageHud(
        private val context: GdxGameContext,
        vm: PersonageViewModel,
        private val tw: Float
) : Group() {

    init {
        name = vm.id.toString()
    }

    val portrait: Image = Image(context.atlas.findRegion(vm.portrait)).apply {
        width = tw / 2
        height = tw / 4
    }

    val healthBarBg = Image(context.atlas.findRegion("resist_bar_black")).apply {
        width = tw / 2
        height = tw / 12
        x = tw / 2
        y = 0f
    }
    val healthBarFg = Image(context.atlas.findRegion("health_bar_green")).apply {
        width = tw / 2
        height = tw / 12
        x = tw / 2
        y = 0f
    }

    val armorBarBg = Image(context.atlas.findRegion("resist_bar_black")).apply {
        width = tw / 2
        height = tw / 12
        x = tw / 2
        y = tw / 12
    }
    val armorBarFg = Image(context.atlas.findRegion("armor_bar_grey")).apply {
        width = tw / 2
        height = tw / 12
        x = tw / 2
        y = tw / 12
    }

    val resistBarBg = Image(context.atlas.findRegion("resist_bar_black")).apply {
        width = tw / 2
        height = tw / 12
        x = tw / 2
        y = tw / 6
    }
    val resistBarFg = Image(context.atlas.findRegion("resist_bar_blue")).apply {
        width = tw / 2
        height = tw / 12
        x = tw / 2
        y = tw / 6
    }

    val iconTick = Image(context.atlas.findRegion("icon_attack")).apply {
        x = 0f
        y = tw / 4
        width = tw / 6
        height = tw / 6
    }

    val labelTick = Label("${vm.stats.tick}/${vm.stats.maxTick}", Label.LabelStyle(context.font, Color.WHITE)).apply {
        setFontScale(0.85f/context.scale)
        x = tw / 6
        y = tw / 4 - tw / 12
    }

    fun updateSelf(personage: PersonageViewModel) {
        resistBarFg.scaleX = if (personage.stats.maxMagicDefense > 0) personage.stats.magicDefense.toFloat() / personage.stats.maxMagicDefense else 0f
        armorBarFg.scaleX = if (personage.stats.maxArmor > 0) personage.stats.armor.toFloat() / personage.stats.maxArmor else 0f
        healthBarFg.scaleX = personage.stats.health.toFloat() / personage.stats.maxHealth

        portrait.alpha = if (personage.stats.health > 0) 1f else 0.2f

        iconTick.isVisible = personage.stats.health > 0 && personage.stats.tickAbility != null
        personage.stats.tickAbility?.let { iconTick.drawable = TextureRegionDrawable(context.atlas.findRegion("icon_$it")) }
        labelTick.isVisible = personage.stats.health > 0 && iconTick.isVisible
        labelTick.setText("${personage.stats.tick}/${personage.stats.maxTick}")
    }

    init {
        addActor(portrait)
        addActor(healthBarBg)
        addActor(healthBarFg)
        addActor(armorBarBg)
        addActor(armorBarFg)
        addActor(resistBarBg)
        addActor(resistBarFg)

        addActor(iconTick)
        addActor(labelTick)

        updateSelf(vm)
    }
}