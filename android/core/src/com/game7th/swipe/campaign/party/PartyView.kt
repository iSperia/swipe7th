package com.game7th.swipe.campaign.party

import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.actions.MoveByAction
import com.badlogic.gdx.scenes.scene2d.actions.RunnableAction
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.game7th.metagame.account.AccountService
import com.game7th.metagame.inventory.GearService
import com.game7th.metagame.unit.UnitConfig
import com.game7th.swipe.ScreenContext
import com.game7th.swipe.campaign.plist.PersonageScrollActor

class PartyView(
        private val context: ScreenContext,
        private val accountService: AccountService,
        private val gearService: GearService
) : Group() {

    val browserHeight = context.scale * 150f
    val personagesView = PersonageScrollActor(context, accountService.getPersonages().map { UnitConfig(it.unit, it.level) }, browserHeight, true, -1).apply {
        y = 10f * context.scale
        x = 10f * context.scale
    }
    val personagesScroll = ScrollPane(personagesView).apply {
        y = 10f * context.scale
        x = 10f * context.scale
        width = 460f * context.scale
        height = 150f * context.scale
    }
    val personagesBg = Image(context.uiAtlas.findRegion("ui_dialog")).apply {
        width = 480 * context.scale
        height = 170 * context.scale
    }

    var firstShow = true
    var isDetailsShown = false

    var detailView: PersonageDetailView? = null

    init {
        addActor(personagesBg)
        addActor(personagesScroll)

        personagesView.selectionCallback = { index ->
            //show details or hide details
            //we are probably show detailView
            val needAnimation = detailView == null
            detailView?.remove()
            detailView = PersonageDetailView(context, accountService, gearService, accountService.getPersonages()[index].id)
            addActor(detailView)
            isDetailsShown = true

            detailView?.let { detailView ->
                if (needAnimation) {
                    detailView.y = -240f * context.scale
                    detailView.addAction(MoveByAction().apply {
                        amountY = 240f * context.scale
                        duration = 0.25f
                    })
                    personagesScroll.y = 10f * context.scale
                    personagesBg.y = 0f
                    personagesScroll.addAction(MoveByAction().apply {
                        amountY = 240f * context.scale
                        duration = 0.25f
                    })
                    personagesBg.addAction(MoveByAction().apply {
                        amountY = 240f * context.scale
                        duration = 0.25f
                    })
                }
            }
        }
    }

    override fun act(delta: Float) {
        super.act(delta)

        if (firstShow) {
            firstShow = false
            val shift = browserHeight + 2 * context.scale * 10f
            y -= shift

            addAction(MoveByAction().apply {
                amountY = shift
                duration = 0.25f
            })
        }
    }

    fun animateHide() {
        val shift = browserHeight + 2 * context.scale * 10f + (if (isDetailsShown) 200f * context.scale else 0f)
        detailView?.hide()
        addAction(SequenceAction(
                MoveByAction().apply {
                    amountY = -shift
                    duration = 0.25f

                },
                RunnableAction().apply {
                    setRunnable { this@PartyView.remove() }
                })
        )
    }
}