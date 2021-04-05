package com.game7th.swipe.util

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor

fun Actor.bounds(): Rectangle {
    val zero = Vector2(0f, 0f)
    val coords = this.localToStageCoordinates(zero)
    return Rectangle(coords.x, coords.y, this.width, this.height)
}