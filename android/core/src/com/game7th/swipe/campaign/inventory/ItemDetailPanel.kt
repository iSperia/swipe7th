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

    val bg = Image(context.commonAtlas.createPatch("ui_hor_panel")).apply {
        width = context.scale * 160f
        height = h
        onClick {
            dismisser()
        }
    }

    val itemView = ItemView(context, item, false, 140f * context.scale).apply {
        x = 10f * context.scale
        y = h - 160f * context.scale
        touchable = Touchable.disabled
    }

    val nameLabel = Label(context.texts[item.getName()] ?: item.getName(), Label.LabelStyle(context.regularFont, Color.YELLOW)).apply {
        x = 10f * context.scale
        y = h - 180f * context.scale
        setFontScale(18f * context.scale / 36f)
        width = 140f * context.scale
        height = 20f * context.scale
        setAlignment(Align.center)
        wrap = true
        touchable = Touchable.disabled
    }

    val affixText = Label("", Label.LabelStyle(context.regularFont, Color.WHITE)).apply {
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
                if (item.item.template.gbFlatBody?: 0f > 0) {
                    texts.add(context.texts["inv_affix_flat_body"]?.replace("$", ((item.item.template.gbFlatBody ?: 0f).toInt()).toString()))
                }
                if (item.item.template.gbPercBody?: 0f > 0) {
                    texts.add(context.texts["inv_affix_perc_body"]?.replace("$", ((item.item.template.gbPercBody ?: 0f).toInt()).toString()))
                }
                if (item.item.template.gbFlatSpirit?: 0f > 0) {
                    texts.add(context.texts["inv_affix_flat_spirit"]?.replace("$", ((item.item.template.gbFlatSpirit ?: 0f).toInt()).toString()))
                }
                if (item.item.template.gbPercSpirit?: 0f > 0) {
                    texts.add(context.texts["inv_affix_perc_spirit"]?.replace("$", ((item.item.template.gbPercSpirit ?: 0f).toInt()).toString()))
                }
                if (item.item.template.gbFlatMind?: 0f > 0) {
                    texts.add(context.texts["inv_affix_flat_mind"]?.replace("$", ((item.item.template.gbFlatMind ?: 0f).toInt()).toString()))
                }
                if (item.item.template.gbPercMind?: 0f > 0) {
                    texts.add(context.texts["inv_affix_perc_mind"]?.replace("$", ((item.item.template.gbPercMind ?: 0f).toInt()).toString()))
                }
                if (item.item.template.gbFlatArmor?: 0f > 0) {
                    texts.add(context.texts["inv_affix_flat_armor"]?.replace("$", ((item.item.template.gbFlatArmor ?: 0f).toInt()).toString()))
                }
                if (item.item.template.gbPercArmor?: 0f > 0) {
                    texts.add(context.texts["inv_affix_perc_armor"]?.replace("$", ((item.item.template.gbPercArmor ?: 0f).toInt()).toString()))
                }
                if (item.item.template.gbFlatHp?: 0f > 0) {
                    texts.add(context.texts["inv_affix_flat_hp"]?.replace("$", ((item.item.template.gbFlatHp ?: 0f).toInt()).toString()))
                }
                if (item.item.template.gbPercHp?: 0f > 0) {
                    texts.add(context.texts["inv_affix_perc_hp"]?.replace("$", ((item.item.template.gbPercHp ?: 0f).toInt()).toString()))
                }
                if (item.item.template.gbFlatEvasion?: 0f > 0) {
                    texts.add(context.texts["inv_affix_flat_evasion"]?.replace("$", ((item.item.template.gbFlatEvasion ?: 0f).toInt()).toString()))
                }
                if (item.item.template.gbPercEvasion?: 0f > 0) {
                    texts.add(context.texts["inv_affix_perc_evasion"]?.replace("$", ((item.item.template.gbPercEvasion ?: 0f).toInt()).toString()))
                }
                if (item.item.template.gbFlatRegeneration?: 0f > 0) {
                    texts.add(context.texts["inv_affix_flat_regeneration"]?.replace("$", ((item.item.template.gbFlatRegeneration ?: 0f).toInt()).toString()))
                }
                if (item.item.template.gbPercRegeneration?: 0f > 0) {
                    texts.add(context.texts["inv_affix_perc_regeneration"]?.replace("$", ((item.item.template.gbPercRegeneration ?: 0f).toInt()).toString()))
                }
                if (item.item.template.gbFlatResist?: 0f > 0) {
                    texts.add(context.texts["inv_affix_flat_resist"]?.replace("$", ((item.item.template.gbFlatResist ?: 0f).toInt()).toString()))
                }
                if (item.item.template.gbPercResist?: 0f > 0) {
                    texts.add(context.texts["inv_affix_perc_resist"]?.replace("$", ((item.item.template.gbPercResist ?: 0f).toInt()).toString()))
                }
                if (item.item.template.gbFlatWisdom?: 0f > 0) {
                    texts.add(context.texts["inv_affix_flat_wisdom"]?.replace("$", ((item.item.template.gbFlatWisdom ?: 0f).toInt()).toString()))
                }
                if (item.item.template.gbPercWisdom?: 0f > 0) {
                    texts.add(context.texts["inv_affix_perc_wisdom"]?.replace("$", ((item.item.template.gbPercWisdom ?: 0f).toInt()).toString()))
                }
            }
            is ItemViewAdapter.PotionItemAdater -> {
                if (item.potion.template.fbFlatHeal?:0 > 0) {
                    texts.add(context.texts["flask_affix_flat_heal"]?.replace("$", item.potion.template.fbFlatHeal.toString()))
                }
                if (item.potion.template.fbRemoveStun?:0 > 0) {
                    texts.add(context.texts["flask_affix_remove_stun"].toString())
                }
            }
        }


        return texts.joinToString("\n")
    }
}