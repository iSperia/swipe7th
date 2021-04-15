package com.game7th.swipe.campaign.party

import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.actions.MoveByAction
import com.badlogic.gdx.scenes.scene2d.actions.RunnableAction
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.game7th.metagame.account.AccountService
import com.game7th.metagame.inventory.GearService
import com.game7th.metagame.dto.UnitConfig
import com.game7th.metagame.dto.UnitType
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.campaign.plist.PersonageScrollActor
import kotlinx.coroutines.launch
import ktx.async.KtxAsync

class PartyView(
        private val context: GdxGameContext,
        private val accountService: AccountService,
        private val gearService: GearService
) : Group() {

    val browserHeight = context.scale * 150f

    lateinit var personagesView:PersonageScrollActor
    lateinit var personagesScroll: ScrollPane

    val personagesBg = Image(context.uiAtlas.createPatch("ui_hor_panel")).apply {
        width = 480 * context.scale
        height = 170 * context.scale
    }

    var firstShow = true
    var isDetailsShown = false

    var detailView: PersonageDetailView? = null

    init {
        KtxAsync.launch {
            personagesView = PersonageScrollActor(context, accountService.getPersonages().map { UnitConfig(UnitType.valueOf(it.unit), it.level) }, browserHeight, true, -1).apply {
                y = 10f * context.scale
                x = 10f * context.scale
            }
            personagesScroll = ScrollPane(personagesView).apply {
                y = 10f * context.scale
                x = 10f * context.scale
                width = 460f * context.scale
                height = 150f * context.scale
            }

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