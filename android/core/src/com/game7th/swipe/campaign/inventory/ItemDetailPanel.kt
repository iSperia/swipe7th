package com.game7th.swipe.campaign.inventory

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.util.ActionPanel
import com.game7th.swipe.util.InventoryAction
import ktx.actors.onClick

class ItemDetailPanel(
        private val context: GdxGameContext,
        val item: ItemViewAdapter,
        private val actions: List<InventoryAction>,
        private val dismisser: () -> Unit,
        private val equipper: (actionIndex: Int, meta: String?) -> Unit
) : Group() {

    val h = (230 + 40 * actions.size) * context.scale

    var meta: String? = null

    val bg = Image(context.uiAtlas.findRegion("ui_dialog")).apply {
        width = context.scale * 160f
        height = h
        onClick {
            dismisser()
        }
    }

    val itemView = ItemView(context, item, false).apply {
        x = 10f * context.scale
        y = h - 160f * context.scale
        setScale(140f/60f)
        touchable = Touchable.disabled
    }

    val nameLabel = Label(context.texts[item.getName()] ?: item.getName(), Label.LabelStyle(context.font, Color.BLACK)).apply {
        x = 10f * context.scale
        y = h - 180f * context.scale
        setFontScale(18f * context.scale / 36f)
        width = 140f * context.scale
        height = 20f * context.scale
        setAlignment(Align.center)
        touchable = Touchable.disabled
    }

    val affixText = Label("", Label.LabelStyle(context.font, Color.BLUE)).apply {
        x = 10f * context.scale
        y = h - 210f * context.scale
        width = 140f * context.scale
        height = 30f * context.scale
        setFontScale(25f * context.scale / 36f)
        touchable = Touchable.disabled
        setAlignment(Align.left)
    }

    val actionGroup = ActionPanel(context, context.scale * 140f, actions, { index: Int -> equipper(index, meta) }).apply {
        y = 10f * context.scale
        x = 10f * context.scale
    }

    init {
        width = bg.width

        addActor(bg)
        addActor(itemView)
        addActor(nameLabel)
        addActor(actionGroup)
        addActor(affixText)

        affixText.setText(getAffixText())
    }

    private fun getAffixText(): String {
        val texts = mutableListOf<String?>()
        when (item) {
            is ItemViewAdapter.InventoryItemAdapter -> {
                if (item.item.gbFlatBody > 0) {
                    texts.add(context.texts["inv_affix_flat_body"]?.replace("$", (item.item.gbFlatBody * item.item.level).toString()))
                }
                if (item.item.gbPercBody > 0) {
                    texts.add(context.texts["inv_affix_perc_body"]?.replace("$", (item.item.gbPercBody * item.item.level).toString()))
                }
                if (item.item.gbFlatSpirit > 0) {
                    texts.add(context.texts["inv_affix_flat_spirit"]?.replace("$", (item.item.gbFlatSpirit * item.item.level).toString()))
                }
                if (item.item.gbPercSpirit > 0) {
                    texts.add(context.texts["inv_affix_perc_spirit"]?.replace("$", (item.item.gbPercSpirit * item.item.level).toString()))
                }
                if (item.item.gbFlatMind > 0) {
                    texts.add(context.texts["inv_affix_flat_mind"]?.replace("$", (item.item.gbFlatMind * item.item.level).toString()))
                }
                if (item.item.gbPercMind > 0) {
                    texts.add(context.texts["inv_affix_perc_mind"]?.replace("$", (item.item.gbPercMind * item.item.level).toString()))
                }
                if (item.item.gbFlatArmor > 0) {
                    texts.add(context.texts["inv_affix_flat_armor"]?.replace("$", (item.item.gbFlatArmor * item.item.level).toString()))
                }
                if (item.item.gbPercArmor > 0) {
                    texts.add(context.texts["inv_affix_perc_armor"]?.replace("$", (item.item.gbPercArmor * item.item.level).toString()))
                }
                if (item.item.gbFlatHp > 0) {
                    texts.add(context.texts["inv_affix_flat_hp"]?.replace("$", (item.item.gbFlatHp * item.item.level).toString()))
                }
                if (item.item.gbPercHp > 0) {
                    texts.add(context.texts["inv_affix_perc_hp"]?.replace("$", (item.item.gbPercHp * item.item.level).toString()))
                }
                if (item.item.gbFlatEvasion > 0) {
                    texts.add(context.texts["inv_affix_flat_evasion"]?.replace("$", (item.item.gbFlatEvasion * item.item.level).toString()))
                }
                if (item.item.gbPercEvasion > 0) {
                    texts.add(context.texts["inv_affix_perc_evasion"]?.replace("$", (item.item.gbPercEvasion * item.item.level).toString()))
                }
                if (item.item.gbFlatRegeneration > 0) {
                    texts.add(context.texts["inv_affix_flat_regeneration"]?.replace("$", (item.item.gbFlatRegeneration * item.item.level).toString()))
                }
                if (item.item.gbPercRegeneration > 0) {
                    texts.add(context.texts["inv_affix_perc_regeneration"]?.replace("$", (item.item.gbPercRegeneration * item.item.level).toString()))
                }
                if (item.item.gbFlatResist > 0) {
                    texts.add(context.texts["inv_affix_flat_resist"]?.replace("$", (item.item.gbFlatResist * item.item.level).toString()))
                }
                if (item.item.gbPercResist > 0) {
                    texts.add(context.texts["inv_affix_perc_resist"]?.replace("$", (item.item.gbPercResist * item.item.level).toString()))
                }
                if (item.item.gbFlatWisdom > 0) {
                    texts.add(context.texts["inv_affix_flat_wisdom"]?.replace("$", (item.item.gbFlatWisdom * item.item.level).toString()))
                }
                if (item.item.gbPercWisdom > 0) {
                    texts.add(context.texts["inv_affix_perc_wisdom"]?.replace("$", (item.item.gbPercWisdom * item.item.level).toString()))
                }
            }
            is ItemViewAdapter.PotionItemAdater -> {
                if (item.potion.template.fbFlatHeal > 0) {
                    texts.add(context.texts["flask_affix_flat_heal"]?.replace("$", item.potion.template.fbFlatHeal.toString()))
                }
                if (item.potion.template.fbRemoveStun > 0) {
                    texts.add(context.texts["flask_affix_remove_stun"].toString())
                }
            }
        }


        return texts.joinToString("\n")
    }
}