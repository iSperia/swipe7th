package com.game7th.swipe.game

import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.game7th.battle.balance.SwipeBalance

data class GdxGameContext(
        val atlas: TextureAtlas,
        val font: BitmapFont,
        val balance: SwipeBalance
)