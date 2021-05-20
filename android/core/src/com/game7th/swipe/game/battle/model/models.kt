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
        val effect: EffectGdxModel?,
        val pose: String?
)

data class FigureGdxModel(
    val name: String,
    val atlas: String,
    val dependencies: List<String>?,
    val height: Int,
    val width: Int,
    var scale: Float,
    val body: String,
    val poses: List<PoseGdxModel>,
    val attacks: List<AttackGdxModel>,
    val source_width: Float,
    val source_height: Float,
    val anchor_x: Float
)

data class EffectGdxModel(
        val name: String,
        val atlas: String,
        val step: Int?,
        val type: String,
        val sound: String?,
        val time: Float = 0f,
        val width: Int = 0,
        val height: Int = 0,
        val trigger: Int = 0,
        val anchor_x: Int? = 0,
        val anchor_y: Int? = 0,
        val scale: Float? = 0f
)

data class GdxModel(
        val figures: List<FigureGdxModel>,
        val ailments: List<EffectGdxModel>
) {
    fun figure(name: String) = figures.firstOrNull { it.name == name }
}
