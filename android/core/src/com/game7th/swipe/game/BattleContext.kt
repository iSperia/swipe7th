package com.game7th.swipe.game

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.esotericsoftware.spine.SkeletonData
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.game.battle.model.GdxModel

data class BattleContext(
        val gameContext: GdxGameContext,
        val battleAtlas: TextureAtlas,
        val locationAtlas: TextureAtlas,
        val gdxModel: GdxModel,
        val width: Float,
        val height: Float,
        val scale: Float,
        val atlases: Map<String, TextureAtlas>,
        val skeletons: Map<String, SkeletonData>,
        val figuresUi: Map<String, TextureAtlas>
)