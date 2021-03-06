package com.game7th.swipe.campaign.party

import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.actions.MoveByAction
import com.badlogic.gdx.scenes.scene2d.actions.RunnableAction
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.game7th.metagame.account.AccountService
import com.game7th.metagame.unit.UnitConfig
import com.game7th.swipe.ScreenContext
import com.game7th.swipe.campaign.plist.PersonageScrollActor

class PartyView(
        private val context: ScreenContext,
        private val service: AccountService
) : Group() {

    val browserHeight = context.scale * 150f
    val personagesView = PersonageScrollActor(context, service.getPersonages().map { UnitConfig(it.unit, it.level) }, browserHeight, true, -1)
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

    var detailView: PersonageTabView? = null

    init {
        addActor(personagesBg)
        addActor(personagesScroll)

        personagesView.selectionCallback = { index ->
            //show details or hide details
            //we are probably show detailView
            val needAnimation = detailView == null
            detailView?.remove()
            detailView = PersonageTabView(context, service, service.getPersonages()[index].id)
            addActor(detailView)
            isDetailsShown = true

            detailView?.let { detailView ->
                if (needAnimation) {
                    detailView.y = -200f * context.scale
                    detailView.addAction(MoveByAction().apply {
                        amountY = 200f * context.scale
                        duration = 0.25f
                    })
                    personagesScroll.y = 0f
                    personagesBg.y = 0f
                    personagesScroll.addAction(MoveByAction().apply {
                        amountY = 200f * context.scale
                        duration = 0.25f
                    })
                    personagesBg.addAction(MoveByAction().apply {
                        amountY = 200f * context.scale
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