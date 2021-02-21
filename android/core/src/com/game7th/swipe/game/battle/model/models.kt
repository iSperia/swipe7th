package com.game7th.swipe.game.battle.model

data class PoseGdxModel(
        val name: String,
        val start: Int,
        val end: Int
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

data class GdxModel(
        val figures: List<FigureGdxModel>,
        val effects: List<EffectGdxModel>
) {
    fun figure(name: String) = figures.firstOrNull { it.name == name }
    fun effect(name: String) = effects.firstOrNull { it.name == name }
}

internal fun String.mapNameToFigure(): String {
    return when (this) {
        "personage_gladiator" -> "body_gladiator"
        "personage_slime" -> "slime_body"
        else -> "slime_body"
    }
}
