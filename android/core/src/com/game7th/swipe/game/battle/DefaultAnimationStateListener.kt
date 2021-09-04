package com.game7th.swipe.game.battle

import com.esotericsoftware.spine.AnimationState
import com.esotericsoftware.spine.Event

open class DefaultAnimationStateListener : AnimationState.AnimationStateListener {

    override fun start(entry: AnimationState.TrackEntry?) {}

    override fun interrupt(entry: AnimationState.TrackEntry?) {}

    override fun end(entry: AnimationState.TrackEntry?) {}

    override fun dispose(entry: AnimationState.TrackEntry?) {}

    override fun complete(entry: AnimationState.TrackEntry?) {}

    override fun event(entry: AnimationState.TrackEntry?, event: Event?) {}
}