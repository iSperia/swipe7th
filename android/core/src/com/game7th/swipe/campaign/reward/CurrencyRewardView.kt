package com.game7th.swipe.campaign.reward

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.game7th.swipe.GdxGameContext
import com.game7th.swiped.api.Currency

class CurrencyRewardView(
        private val context: GdxGameContext,
        private val currency: Currency,
        private val amount: Int,
        val size: Float = context.scale * 60f
) : Group() {

    val bg = Image(context.uiAtlas.findRegion("ui_item_bg")).apply {
        width = size
        height = size
    }

    val image: Image? = Image(context.uiAtlas.findRegion(getTextureName(currency))).apply {
        width = size
        height = size
    }

    val lvlLabel = Label(amount.toString(), Label.LabelStyle(context.font, Color.WHITE)).apply {
        x = 25f * context.scale
        y = 5f
        width = size / 2
        height = size / 2
        setAlignment(Align.bottomRight)
        setFontScale(size / 4 / 36f)
    }

    companion object {
        fun getTextureName(currency: Currency): String {
            return when (currency) {
                Currency.GEMS -> "ui_currency_gems"
                Currency.GOLD -> "ui_currency_gold"
                Currency.DUST -> "ui_currency_dust"
                else -> "ui_currency_gems"
            }
        }
    }

    init {
        addActor(bg)
        image?.let { addActor(it) }
        addActor(lvlLabel)

        width = size
        height = size
    }
}