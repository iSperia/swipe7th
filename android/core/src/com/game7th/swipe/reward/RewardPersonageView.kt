package com.game7th.swipe.reward

import com.badlogic.gdx.scenes.scene2d.Group
import com.game7th.swipe.BaseScreen
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.campaign.plist.PersonageVerticalPortrait
import com.game7th.swipe.campaign.plist.PortraitConfig
import com.game7th.swiped.api.PackEntryDto

class RewardPersonageView(
        private val context: GdxGameContext,
        private val screen: BaseScreen,
        private val item: PackEntryDto
) : Group() {

    val portrait = PersonageVerticalPortrait(context, PortraitConfig(item.meta, "vp_${item.meta.toLowerCase()}", 1), context.scale * 90f)

    init {
        addActor(portrait)
        screen.personagesUpdated()
    }
}