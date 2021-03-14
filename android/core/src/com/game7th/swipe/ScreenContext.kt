package com.game7th.swipe

import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.game7th.battle.balance.SwipeBalance

class ScreenContext(
        val uiAtlas: TextureAtlas,
        val font: BitmapFont,
        val battleAtlas: TextureAtlas,
        val scale: Float,
        val balance: SwipeBalance
)