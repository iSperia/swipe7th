package com.game7th.swipe.campaign.top_menu

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.game7th.metagame.account.AccountService
import com.game7th.swipe.GdxGameContext
import com.game7th.swiped.api.Currency
import kotlinx.coroutines.launch
import ktx.async.KtxAsync

class CurrencyPanel(
        private val context: GdxGameContext,
        private val accountService: AccountService
) : Group() {

    val background = Image(context.commonAtlas.findRegion("panel_blue")).apply {
        width = 178f * context.scale
        height = 34f * context.scale
    }

    val iconGold = Image(context.commonAtlas.findRegion("ui_currency_gold")).apply {
        width = 24f * context.scale
        height = 24f * context.scale
        x = 5f * context.scale
        y = 5f * context.scale
    }

    val iconGems = Image(context.commonAtlas.findRegion("ui_currency_gems")).apply {
        width = 24f * context.scale
        height = 24f * context.scale
        x = 101f * context.scale
        y = 5f * context.scale
    }

    lateinit var balance: Map<String, Int>
    lateinit var labelGold: Label
    lateinit var labelGems: Label

    init {
        addActor(background)
        addActor(iconGold)
        addActor(iconGems)

        KtxAsync.launch {
            balance = accountService.getBalance()

            labelGold = Label((balance[Currency.GOLD.toString()] ?: 0).toString(), Label.LabelStyle(context.regularFont, Color.WHITE)).apply {
                x = 34f * context.scale
                y = 5f * context.scale
                width = 62f * context.scale
                height = 24f * context.scale
                setAlignment(Align.left)
                setFontScale(20f * context.scale / 36f)
            }

            labelGems = Label((balance[Currency.GEMS.toString()] ?: 0).toString(), Label.LabelStyle(context.regularFont, Color.WHITE)).apply {
                x = 130f * context.scale
                y = 5f * context.scale
                width = 38f * context.scale
                height = 24f * context.scale
                setAlignment(Align.left)
                setFontScale(20f * context.scale / 36f)
            }
            addActor(labelGold)
            addActor(labelGems)
        }
    }

    fun refreshBalance() {
        KtxAsync.launch {
            accountService.refreshBalance()
            balance = accountService.getBalance()
            labelGold.setText((balance[Currency.GOLD.toString()] ?: 0).toString())
            labelGems.setText((balance[Currency.GEMS.toString()] ?: 0).toString())
        }
    }
}