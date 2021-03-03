package com.game7th.swipe

import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.game7th.battle.balance.SwipeBalance

data class GdxGameContext(
        val atlas: TextureAtlas,
        val font: BitmapFont,
        val balance: SwipeBalance,
        val scale: Float,
        val atlases: Map<String, TextureAtlas>
)