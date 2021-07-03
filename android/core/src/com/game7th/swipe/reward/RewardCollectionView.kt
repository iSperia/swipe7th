package com.game7th.swipe.reward

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.game7th.swipe.BaseScreen
import com.game7th.swipe.GdxGameContext
import com.game7th.swiped.api.PackEntryDto
import com.game7th.swiped.api.PackEntryType
import ktx.actors.onClick

class RewardCollectionView(
        private val context: GdxGameContext,
        private val screen: BaseScreen,
        private val rewards: List<PackEntryDto>,
        private val titleResId: String
): Group() {

    val background = Image(context.commonAtlas.findRegion("panel_modal")).apply {
        width = Gdx.graphics.width.toFloat()
        height = Gdx.graphics.height.toFloat()
        onClick {  }
    }

    val titleLabel = Label(context.texts[titleResId] ?: titleResId, Label.LabelStyle(context.regularFont, Color.YELLOW)).apply {
        y = Gdx.graphics.height * 0.6f
        x = 40f
        width = 400f * context.scale
        height = 36f * context.scale
        setFontScale(context.scale)
        setAlignment(Align.center)
        touchable = Touchable.disabled
    }

    val closeLabel = Label("Close", Label.LabelStyle(context.regularFont, Color.WHITE)).apply {
        y = Gdx.graphics.height * 0.3f
        x = 40f
        width = 400f * context.scale
        height = 36f * context.scale
        setFontScale(context.scale)
        setAlignment(Align.center)
        onClick {
            this@RewardCollectionView.remove()
        }
    }

    private val padding = (Gdx.graphics.width - 72f * context.scale * rewards.size) / 2f

    init {
        addActor(background)
        addActor(titleLabel)
        addActor(closeLabel)

        rewards.forEachIndexed { index, packEntryDto ->
            when (packEntryDto.entryType) {
                PackEntryType.ITEM, PackEntryType.CURRENCY, PackEntryType.FLASK -> {
                    val packItemView = RewardItemView(context, screen, packEntryDto).apply {
                        x = padding + 6f * context.scale + 72f * index * context.scale
                        y = titleLabel.y - 120f * context.scale
                    }
                    addActor(packItemView)
                }
                PackEntryType.PERSONAGE -> {
                    val packItemView = RewardPersonageView(context, screen, packEntryDto).apply {
                        x = padding + 6f * context.scale + 72f * index * context.scale
                        y = titleLabel.y - 120f * context.scale
                    }
                    addActor(packItemView)
                }
            }

        }
    }
}