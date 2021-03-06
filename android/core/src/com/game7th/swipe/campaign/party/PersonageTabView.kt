package com.game7th.swipe.campaign.party

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.game7th.metagame.account.AccountService
import com.game7th.metagame.unit.UnitConfig
import com.game7th.swipe.ScreenContext
import com.game7th.swipe.campaign.plist.PersonageVerticalPortrait
import kotlin.math.exp

class PersonageTabView(
        private val context: ScreenContext,
        private val service: AccountService,
        private val personageId: Int
) : Group() {

    private val personage = service.getPersonages().first { it.id == personageId }

    val bg = Image(context.uiAtlas.findRegion("ui_dialog")).apply {
        width = 480f * context.scale
        height = 240f * context.scale
    }

    val portrait = PersonageVerticalPortrait(context, UnitConfig(personage.unit, personage.level), 180f * context.scale).apply {
        x = 10f * context.scale
        y = 50f * context.scale
    }

    val attrsBg = Image(context.uiAtlas.findRegion("ui_attrs_tree")).apply {
        x = 140f * context.scale
        y = 10f * context.scale
        width = 160f * context.scale
        height = 160f * context.scale
    }

    val sh = attrsBg.height / 3f

    val ah = 0.08f * attrsBg.height
    val aw = 0.13f * attrsBg.height

    val bodyLabel = Label(personage.stats.body.toString(), Label.LabelStyle(context.font, Color.WHITE)).apply {
        x = attrsBg.x + 0.19f * attrsBg.width
        y = attrsBg.y + 0.04f * attrsBg.height
        width = aw
        height = ah
        setAlignment(Align.center)
        setFontScale(ah / 36f)
    }

    val spiritLabel = Label(personage.stats.spirit.toString(), Label.LabelStyle(context.font, Color.WHITE)).apply {
        x = attrsBg.x + 0.45f * attrsBg.width
        y = attrsBg.y + 0.87f * attrsBg.height
        width = aw
        height = ah
        setAlignment(Align.center)
        setFontScale(ah / 36f)
    }

    val mindLabel = Label(personage.stats.mind.toString(), Label.LabelStyle(context.font, Color.WHITE)).apply {
        x = attrsBg.x + 0.7f * attrsBg.width
        y = attrsBg.y + 0.04f * attrsBg.height
        width = aw
        height = ah
        setAlignment(Align.center)
        setFontScale(ah / 36f)
    }

    val secondAttrsBody = Label("Health: ${context.balance.calculateHealth(personage)}\nArmor: ${context.balance.calculateArmor(personage)}", Label.LabelStyle(context.font, Color.RED)).apply {
        y = attrsBg.y + 2 * sh
        x = attrsBg.x + attrsBg.width + context.scale * 0.1f
        width = 140f * context.scale
        height = sh
        setFontScale(sh / 2.3f / 36f)
        setAlignment(Align.left)
    }

    val secondAttrsSpirit = Label("Evasion: ${context.balance.calculateEvasion(personage)}\nRegeneration: ${context.balance.calculateRegeneration(personage)}", Label.LabelStyle(context.font, Color.FOREST)).apply {
        y = attrsBg.y + sh
        x = attrsBg.x + attrsBg.width + context.scale * 0.1f
        width = 140f * context.scale
        height = sh
        setFontScale(sh / 2.3f / 36f)
        setAlignment(Align.left)
    }

    val secondAttrsMind = Label("Resist: ${context.balance.calculateResist(personage)}\nWisdom: ${context.balance.calculateWisdom(personage)}", Label.LabelStyle(context.font, Color.BLUE)).apply {
        y = attrsBg.y
        x = attrsBg.x + attrsBg.width + context.scale * 0.1f
        width = 140f * context.scale
        height = sh
        setFontScale(sh / 2.3f / 36f)
        setAlignment(Align.left)
    }

    val nextLevelExp = ((personage.level - 1) + exp(personage.level * 0.1f)).toInt() * personage.level * 50

    val experienceBar = ExperienceBar(context, 120f * context.scale, 30f * context.scale, personage.experience, nextLevelExp).apply {
        x = 10f * context.scale
        y = 10f * context.scale
    }

    init {
        addActor(bg)
        addActor(portrait)
        addActor(attrsBg)

        addActor(bodyLabel)
        addActor(spiritLabel)
        addActor(mindLabel)

        addActor(secondAttrsBody)
        addActor(secondAttrsSpirit)
        addActor(secondAttrsMind)

        addActor(experienceBar)
    }
}