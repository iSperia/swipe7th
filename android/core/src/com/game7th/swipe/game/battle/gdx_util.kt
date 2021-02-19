package com.game7th.swipe.game.battle

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array

fun filterAtlas(atlas: TextureAtlas, prefix: String): Array<TextureRegion> {
    return atlas.regions
            .filter { it.name.startsWith(prefix) }
            .toList()
            .sortedBy { it.name }
            .let { Array(it.toTypedArray()) }
}