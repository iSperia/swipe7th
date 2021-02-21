package com.game7th.swipe.game.battle.model

data class PoseGdxModel(
        val name: String,
        val startFrame: Int,
        val endFrame: Int
)

data class FigureGdxModel(
    val name: String,
    val atlas: String,
    val poses: List<PoseGdxModel>
)

enum class EffectType {
    STEPPED_GENERATOR
}

data class EffectGdxModel(
        val name: String,
        val atlas: String,
        val step: Int?,
        val time: Float,
        val width: Int,
        val height: Int
)