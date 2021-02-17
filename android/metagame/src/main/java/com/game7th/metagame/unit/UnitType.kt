package com.game7th.metagame.unit

enum class UnitType {
    GLADIATOR,
    POISON_ARCHER,
    MACHINE_GUNNER,

    GREEN_SLIME,
    CITADEL_WARLOCK,
    FLAME_ELEMENT,
    EARTH_ELEMENT,
    BOSS_BLOOD_KNIGHT,

    UNKNOWN;

    fun getSkin(): String = when (this) {
        UnitType.GLADIATOR -> "p_gladiator"
        UnitType.POISON_ARCHER -> "personage_ranger"
        UnitType.GREEN_SLIME -> "personage_slime"
        UnitType.MACHINE_GUNNER -> "personage_gunner"
        UnitType.CITADEL_WARLOCK -> "personage_citadel_warlock"
        else -> "personage_dead"
    }
}