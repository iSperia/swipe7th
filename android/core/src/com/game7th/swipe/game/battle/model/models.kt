package com.game7th.swipe.game.battle.model

data class PoseGdxModel(
        val name: String,
        val start: Int,
        val end: Int,
        val triggers: List<Int>?
)

enum class GdxAttackType {
    MOVE_AND_PUNCH,
    AOE_STEPPED_GENERATOR
}

data class AttackGdxModel(
        val attackType: GdxAttackType,
        val effect: EffectGdxModel?
)

data class FigureGdxModel(
    val name: String,
    val atlas: String,
    val height: Int,
    val poses: List<PoseGdxModel>,
    val attacks: List<AttackGdxModel>
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

data class GdxModel(
        val figures: List<FigureGdxModel>
) {
    fun figure(name: String) = figures.firstOrNull { it.name == name }
}

internal fun String.mapNameToFigure(): String {
    return when (this) {
        "personage_gladiator" -> "body_gladiator"
        "personage_slime" -> "slime_body"
        "personage_red_slime" -> "slime_red_body"
        "personage_slime_mother" -> "slime_mother_body"
        else -> "slime_body"
    }
}

sealed abstract class BattleControllerEvent {
    data class FigurePoseFrameIndexEvent(val figureId: Int, val frame: Int) : BattleControllerEvent()
    data class FigurePoseEndedEvent(val figureId: Int) : BattleControllerEvent()
    data class SteppedGeneratorEvent(val index: Int): BattleControllerEvent()
}
