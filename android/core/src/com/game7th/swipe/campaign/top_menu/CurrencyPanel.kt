package com.game7th.swipe.campaign.top_menu

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.game7th.metagame.account.AccountService
import com.game7th.metagame.account.dto.Currency
import com.game7th.swipe.GdxGameContext

class CurrencyPanel(
        private val context: GdxGameContext,
        private val accountService: AccountService
) : Group() {

    val background = Image(context.uiAtlas.findRegion("panel_blue")).apply {
        width = 178f * context.scale
        height = 34f * context.scale
    }

    val iconGold = Image(context.uiAtlas.findRegion("ui_currency_gold")).apply {
        width = 24f * context.scale
        height = 24f * context.scale
        x = 5f * context.scale
        y = 5f * context.scale
    }

    val iconGems = Image(context.uiAtlas.findRegion("ui_currency_gems")).apply {
        width = 24f * context.scale
        height = 24f * context.scale
        x = 101f * context.scale
        y = 5f * context.scale
    }

    var balance = accountService.getBalance()

    val labelGold = Label((balance.currencies[Currency.GOLD] ?: 0).toString(), Label.LabelStyle(context.font, Color.WHITE)).apply {
        x = 34f * context.scale
        y = 5f * context.scale
        width = 62f * context.scale
        height = 24f * context.scale
        setAlignment(Align.left)
        setFontScale(20f * context.scale / 36f)
    }

    val labelGems = Label((balance.currencies[Currency.GEMS] ?: 0).toString(), Label.LabelStyle(context.font, Color.WHITE)).apply {
        x = 130f * context.scale
        y = 5f * context.scale
        width = 38f * context.scale
        height = 24f * context.scale
        setAlignment(Align.left)
        setFontScale(20f * context.scale / 36f)
    }

    init {
        addActor(background)
        addActor(iconGold)
        addActor(iconGems)
        addActor(labelGold)
        addActor(labelGems)
    }

    fun refreshBalance() {
        balance = accountService.getBalance()
        labelGold.setText((balance.currencies[Currency.GOLD] ?: 0).toString())
        labelGems.setText((balance.currencies[Currency.GEMS] ?: 0).toString())
    }
}