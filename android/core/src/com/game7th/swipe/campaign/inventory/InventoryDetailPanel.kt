package com.game7th.swipe.campaign.inventory

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.game7th.metagame.account.dto.Currency
import com.game7th.metagame.inventory.dto.InventoryItem
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.campaign.reward.CurrencyRewardView
import ktx.actors.onClick

sealed class InventoryAction {
    data class StringAction(val text: String): InventoryAction()
    data class IconAction(val text: String, val icon: String, val currency: Currency): InventoryAction()
}

class InventoryDetailPanel(
        private val context: GdxGameContext,
        private val item: InventoryItem,
        private val actions: List<InventoryAction>,
        private val dismisser: () -> Unit,
        private val equipper: (item: InventoryItem, actionIndex: Int) -> Unit
) : Group() {

    val h = (230 + 40 * actions.size) * context.scale

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

    val nameLabel = Label(item.name, Label.LabelStyle(context.font, Color.BLACK)).apply {
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

    val actionGroup = Group().apply {
        y = 10f * context.scale
        x = 10f * context.scale
    }



    init {
        addActor(bg)
        addActor(itemView)
        addActor(nameLabel)
        addActor(actionGroup)
        addActor(affixText)

        actions.forEachIndexed { index, action ->

            when (action) {
                is InventoryAction.StringAction -> {
                    val button = Button(Button.ButtonStyle(TextureRegionDrawable(context.uiAtlas.findRegion("ui_button_simple")),
                            TextureRegionDrawable(context.uiAtlas.findRegion("ui_button_pressed")), null)).apply {
                        y = index * 40f * context.scale
                        width = 140f * context.scale
                        height = 30f * context.scale
                        onClick { equipper(item, index) }
                    }

                    val label = Label(action.text, Label.LabelStyle(context.font, Color.BLACK)).apply {
                        x = button.x
                        y = button.y
                        width = button.width
                        height = button.height
                        setFontScale(24f * context.scale / 36f)
                        setAlignment(Align.center)
                        touchable = Touchable.disabled
                    }
                    actionGroup.addActor(button)
                    actionGroup.addActor(label)
                }

                is InventoryAction.IconAction -> {
                    val button = Button(Button.ButtonStyle(TextureRegionDrawable(context.uiAtlas.findRegion("ui_button_simple")),
                            TextureRegionDrawable(context.uiAtlas.findRegion("ui_button_pressed")), null)).apply {
                        y = index * 40f * context.scale
                        width = 140f * context.scale
                        height = 30f * context.scale
                        onClick { equipper(item, index) }
                    }

                    val label = Label(action.text, Label.LabelStyle(context.font, Color.BLACK)).apply {
                        x = button.x + 30f * context.scale
                        y = button.y
                        height = button.height
                        setFontScale(24f * context.scale / 36f)
                        setAlignment(Align.left)
                        touchable = Touchable.disabled
                    }

                    actionGroup.addActor(button)
                    actionGroup.addActor(label)

                    val actionIcon = Image(context.uiAtlas.findRegion(action.icon)).apply {
                        x = button.x + 3f * context.scale
                        y = button.y + 3f * context.scale
                        width = 24f * context.scale
                        height = 24f * context.scale
                        touchable = Touchable.disabled
                    }
                    actionGroup.addActor(actionIcon)

                    val currencyIcon = Image(context.uiAtlas.findRegion(CurrencyRewardView.getTextureName(action.currency))).apply {
                        x = label.x + label.width + 20f * context.scale
                        y = button.y
                        touchable = Touchable.disabled
                    }
                    actionGroup.addActor(currencyIcon)
                }
            }

        }

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