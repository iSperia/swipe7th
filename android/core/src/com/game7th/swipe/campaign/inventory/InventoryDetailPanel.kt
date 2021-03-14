package com.game7th.swipe.campaign.inventory

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.game7th.metagame.inventory.InventoryItem
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.ScreenContext
import ktx.actors.onClick

class InventoryDetailPanel(
        private val context: GdxGameContext,
        private val item: InventoryItem,
        private val action: String,
        private val dismisser: () -> Unit,
        private val equipper: (item: InventoryItem) -> Unit
) : Group() {

    val bg = Image(context.uiAtlas.findRegion("ui_dialog")).apply {
        width = context.scale * 160f
        height = context.scale * 250f
        onClick {
            dismisser()
        }
    }

    val itemView = ItemView(context, item, false).apply {
        x = 10f * context.scale
        y = 100f * context.scale
        setScale(140f/60f)
        touchable = Touchable.disabled
    }

    val nameLabel = Label(item.name, Label.LabelStyle(context.font, Color.BLACK)).apply {
        x = 10f * context.scale
        y = 80f * context.scale
        setFontScale(18f * context.scale / 36f)
        width = 140f * context.scale
        height = 20f * context.scale
        setAlignment(Align.center)
        touchable = Touchable.disabled
    }

    val affixText = Label("", Label.LabelStyle(context.font, Color.BLUE)).apply {
        x = 10f * context.scale
        y = 40f * context.scale
        width = 140f * context.scale
        height = 30f * context.scale
        setFontScale(25f * context.scale / 36f)
        touchable = Touchable.disabled
        setAlignment(Align.left)
    }

    val equipButton = Button(Button.ButtonStyle(TextureRegionDrawable(context.uiAtlas.findRegion("ui_button_simple")),
        TextureRegionDrawable(context.uiAtlas.findRegion("ui_button_pressed")), null)).apply {
        x = 10f * context.scale
        y = 10f * context.scale
        width = 140f * context.scale
        height = 20f * context.scale
        onClick { equipper(item) }
    }

    val equipLabel = Label(action, Label.LabelStyle(context.font, Color.BLACK)).apply {
        x = equipButton.x
        y = equipButton.y
        width = equipButton.width
        height = equipButton.height
        setFontScale(18f * context.scale / 36f)
        setAlignment(Align.center)
        touchable = Touchable.disabled
    }

    init {
        addActor(bg)
        addActor(itemView)
        addActor(nameLabel)
        addActor(equipButton)
        addActor(affixText)
        addActor(equipLabel)

        affixText.setText(getAffixText())
    }

    private fun getAffixText(): String {
        val texts = mutableListOf<String?>()
        if (item.gbFlatBody > 0) {
            texts.add(context.texts["inv_affix_flat_body"]?.replace("$", (item.gbFlatBody * item.level).toString()))
        }
        if (item.gbPercBody > 0) {
            texts.add(context.texts["inv_affix_perc_body"]?.replace("$", (item.gbPercBody * item.level).toString()))
        }
        if (item.gbFlatSpirit > 0) {
            texts.add(context.texts["inv_affix_flat_spirit"]?.replace("$", (item.gbFlatSpirit * item.level).toString()))
        }
        if (item.gbPercSpirit > 0) {
            texts.add(context.texts["inv_affix_perc_spirit"]?.replace("$", (item.gbPercSpirit * item.level).toString()))
        }
        if (item.gbFlatMind > 0) {
            texts.add(context.texts["inv_affix_flat_mind"]?.replace("$", (item.gbFlatMind * item.level).toString()))
        }
        if (item.gbPercMind > 0) {
            texts.add(context.texts["inv_affix_perc_mind"]?.replace("$", (item.gbPercMind * item.level).toString()))
        }
        if (item.gbFlatArmor > 0) {
            texts.add(context.texts["inv_affix_flat_armor"]?.replace("$", (item.gbFlatArmor * item.level).toString()))
        }
        if (item.gbPercArmor > 0) {
            texts.add(context.texts["inv_affix_perc_armor"]?.replace("$", (item.gbPercArmor * item.level).toString()))
        }
        if (item.gbFlatHp > 0) {
            texts.add(context.texts["inv_affix_flat_hp"]?.replace("$", (item.gbFlatHp * item.level).toString()))
        }
        if (item.gbPercHp > 0) {
            texts.add(context.texts["inv_affix_perc_hp"]?.replace("$", (item.gbPercHp * item.level).toString()))
        }
        if (item.gbFlatEvasion > 0) {
            texts.add(context.texts["inv_affix_flat_evasion"]?.replace("$", (item.gbFlatEvasion * item.level).toString()))
        }
        if (item.gbPercEvasion > 0) {
            texts.add(context.texts["inv_affix_perc_evasion"]?.replace("$", (item.gbPercEvasion * item.level).toString()))
        }
        if (item.gbFlatRegeneration > 0) {
            texts.add(context.texts["inv_affix_flat_regeneration"]?.replace("$", (item.gbFlatRegeneration * item.level).toString()))
        }
        if (item.gbPercRegeneration > 0) {
            texts.add(context.texts["inv_affix_perc_regeneration"]?.replace("$", (item.gbPercRegeneration * item.level).toString()))
        }
        if (item.gbFlatResist > 0) {
            texts.add(context.texts["inv_affix_flat_resist"]?.replace("$", (item.gbFlatResist * item.level).toString()))
        }
        if (item.gbPercResist > 0) {
            texts.add(context.texts["inv_affix_perc_resist"]?.replace("$", (item.gbPercResist * item.level).toString()))
        }
        if (item.gbFlatWisdom > 0) {
            texts.add(context.texts["inv_affix_flat_wisdom"]?.replace("$", (item.gbFlatWisdom * item.level).toString()))
        }
        if (item.gbPercWisdom > 0) {
            texts.add(context.texts["inv_affix_perc_wisdom"]?.replace("$", (item.gbPercWisdom * item.level).toString()))
        }

        return texts.joinToString("\n")
    }
}