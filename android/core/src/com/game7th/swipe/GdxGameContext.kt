package com.game7th.swipe

import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.game7th.metagame.PersistentStorage

data class GdxGameContext(
        val battleAtlas: TextureAtlas,
        val uiAtlas: TextureAtlas,
        val font: BitmapFont,
        val font2: BitmapFont,
        val scale: Float,
        val texts: Map<String, String>,
        val storage: PersistentStorage
)