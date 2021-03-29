package com.game7th.swipe.util

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.actions.MoveByAction
import com.badlogic.gdx.scenes.scene2d.actions.RunnableAction
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import com.game7th.swipe.GdxGameContext

fun Actor.animateShowFromBottom(context: GdxGameContext, h: Float) {
    y = -h * context.scale
    addAction(MoveByAction().apply {
        amountY = 48f * context.scale + h * context.scale
        duration = 0.25f
    })
}

fun Actor.animateHideToBottom(context: GdxGameContext, h: Float) {
    val shift = h * context.scale + 48f * context.scale
    addAction(SequenceAction(
            MoveByAction().apply {
                amountY = -shift
                duration = 0.25f

            },
            RunnableAction().apply {
                setRunnable { remove() }
            })
    )
}