package com.game7th.swipe.campaign.reward

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.game7th.metagame.account.dto.Currency
import com.game7th.swipe.GdxGameContext

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

    val image: Image? = Image(context.uiAtlas.findRegion(getTextureName())).apply {
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

    private fun getTextureName(): String {
        return when (currency) {
            Currency.GEMS -> "ui_currency_gems"
            Currency.GOLD -> "ui_currency_gold"
            else -> "ui_currency_gems"
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