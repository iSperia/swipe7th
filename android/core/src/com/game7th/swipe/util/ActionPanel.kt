package com.game7th.swipe.util

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.utils.Align
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.campaign.reward.CurrencyRewardView
import com.game7th.swiped.api.Currency
import ktx.actors.onClick

sealed class InventoryAction {
    data class StringAction(val text: String): InventoryAction()
    data class IconAction(val text: String, val icon: String, val currency: Currency?): InventoryAction()
}

class ActionPanel(
        private val context: GdxGameContext,
        buttonWidth: Float,
        private val actions: List<InventoryAction>,
        private val equipper: (actionIndex: Int) -> Unit
): Group() {

    val h = actions.size * 40f * context.scale

    init {
        height = h

        actions.forEachIndexed { index, action ->

            when (action) {
                is InventoryAction.StringAction -> {
                    val button = Button(Button.ButtonStyle(NinePatchDrawable(context.uiAtlas.createPatch("ui_button_simple")),
                            NinePatchDrawable(context.uiAtlas.createPatch("ui_button_pressed")), null)).apply {
                        y = index * 40f * context.scale
                        width = buttonWidth
                        height = 30f * context.scale
                        onClick { equipper(index) }
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
                    addActor(button)
                    addActor(label)
                }

                is InventoryAction.IconAction -> {
                    val button = Button(Button.ButtonStyle(NinePatchDrawable(context.uiAtlas.createPatch("ui_button_simple")),
                            NinePatchDrawable(context.uiAtlas.createPatch("ui_button_pressed")), null)).apply {
                        y = index * 40f * context.scale
                        width = buttonWidth
                        height = 30f * context.scale
                        onClick { equipper(index) }
                    }

                    val label = Label(action.text, Label.LabelStyle(context.font, Color.BLACK)).apply {
                        x = button.x + 30f * context.scale
                        y = button.y
                        height = button.height
                        setFontScale(24f * context.scale / 36f)
                        setAlignment(Align.left)
                        touchable = Touchable.disabled
                    }

                    addActor(button)
                    addActor(label)

                    val actionIcon = Image(context.uiAtlas.findRegion(action.icon)).apply {
                        x = button.x + 3f * context.scale
                        y = button.y + 3f * context.scale
                        width = 24f * context.scale
                        height = 24f * context.scale
                        touchable = Touchable.disabled
                    }
                    addActor(actionIcon)

                    if (action.currency != null) {
                        val currencyIcon = Image(context.uiAtlas.findRegion(CurrencyRewardView.getTextureName(action.currency))).apply {
                            x = label.x + label.width + 20f * context.scale
                            y = button.y
                            touchable = Touchable.disabled
                        }
                        addActor(currencyIcon)
                    }
                }
            }

        }
    }
}