package com.game7th.swipe.reward

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.game7th.swipe.BaseScreen
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.campaign.reward.CurrencyRewardView
import com.game7th.swiped.api.Currency
import com.game7th.swiped.api.PackEntryDto
import com.game7th.swiped.api.PackEntryType

class RewardItemView(
        private val context: GdxGameContext,
        private val screen: BaseScreen,
        private val item: PackEntryDto
): Group() {

    val texture = when (item.entryType) {
        PackEntryType.CURRENCY -> context.uiAtlas.findRegion(CurrencyRewardView.getTextureName(Currency.valueOf(item.meta)))
        PackEntryType.ITEM -> context.battleAtlas.findRegion("art_${item.meta}")
        else -> context.uiAtlas.findRegion(CurrencyRewardView.getTextureName(Currency.GOLD))
    }

    val itemImage = Image(texture).apply {
        width = 60f * context.scale
        height = 60f * context.scale
    }

    val label = Label("", Label.LabelStyle(context.font, Color.WHITE)).apply {
        width = 30f * context.scale
        height = 30f * context.scale
        x = 30f * context.scale
        setFontScale(30f * context.scale / 36f)
        setAlignment(Align.center)
    }

    init {
        addActor(itemImage)
        addActor(label)

        label.setText(if (item.level > 0) {
            item.level.toString()
        } else {
            item.amount.toString()
        })

        when (item.entryType) {
            PackEntryType.CURRENCY -> screen.currencyUpdated()
            PackEntryType.ITEM -> screen.inventoryUpdated()
        }
    }
}