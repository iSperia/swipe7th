package com.game7th.swipe.game

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.game7th.swipe.GdxGameContext

data class GameContextWrapper(
        val gameContext: GdxGameContext,
        val width: Float,
        val height: Float,
        val atlases: Map<String, TextureAtlas>
)