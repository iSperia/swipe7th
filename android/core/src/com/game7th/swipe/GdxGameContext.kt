package com.game7th.swipe

import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.game7th.metagame.PersistentStorage

data class GdxGameContext(
        val commonAtlas: TextureAtlas,
        val itemsAtlas: TextureAtlas,
        val regularFont: BitmapFont,
        val captionFont: BitmapFont,
        val scale: Float,
        val texts: Map<String, String>,
        val storage: PersistentStorage
)