package com.game7th.swipe.game.battle.model

enum class PoseAnimationBehavior {
    FREEZE, SWITCH_IDLE, REPEAT
}

data class PoseGdxModel(
        val name: String,
        val startFrame: Int,
        val endFrame: Int,
        val behavior: PoseAnimationBehavior = PoseAnimationBehavior.SWITCH_IDLE
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