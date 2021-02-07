package com.game7th.swipe.gestures

import com.badlogic.gdx.input.GestureDetector

class SimpleDirectionGestureDetector(directionListener: DirectionListener) : GestureDetector(DirectionGestureListener(directionListener)) {
    interface DirectionListener {
        fun onLeft()
        fun onRight()
        fun onUp()
        fun onDown()
    }

    private class DirectionGestureListener(var directionListener: DirectionListener) : GestureAdapter() {
        override fun fling(velocityX: Float, velocityY: Float, button: Int): Boolean {
            if (Math.abs(velocityX) > Math.abs(velocityY)) {
                if (velocityX > 0) {
                    directionListener.onRight()
                } else {
                    directionListener.onLeft()
                }
            } else {
                if (velocityY > 0) {
                    directionListener.onDown()
                } else {
                    directionListener.onUp()
                }
            }
            return super.fling(velocityX, velocityY, button)
        }
    }
}