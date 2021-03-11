package com.game7th.swipe.game.battle.model

data class PoseGdxModel(
        val name: String,
        val start: Int,
        val end: Int,
        val triggers: List<Int>?,
        val sound: String?
)

enum class GdxAttackType {
    MOVE_AND_PUNCH,
    AOE_STEPPED_GENERATOR,
    BOW_STRIKE,
    ATTACK_IN_PLACE
}

data class AttackGdxModel(
        val attackType: GdxAttackType,
        val sound: String?,
        val effect: EffectGdxModel?
)

data class FigureGdxModel(
    val name: String,
    val atlas: String,
    val dependencies: List<String>?,
    val height: Int,
    val body: String,
    val poses: List<PoseGdxModel>,
    val attacks: List<AttackGdxModel>
)

data class EffectGdxModel(
        val name: String,
        val atlas: String,
        val step: Int?,
        val sound: String?,
        val time: Float = 0f,
        val width: Int = 0,
        val height: Int = 0,
        val trigger: Int = 0
)

data class GdxModel(
        val figures: List<FigureGdxModel>,
        val ailments: List<EffectGdxModel>
) {
    fun figure(name: String) = figures.firstOrNull { it.name == name }
}
