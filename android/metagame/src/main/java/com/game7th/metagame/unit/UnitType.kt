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
        GLADIATOR -> "p_gladiator"
        POISON_ARCHER -> "personage_ranger"
        GREEN_SLIME -> "personage_slime"
        MACHINE_GUNNER -> "personage_gunner"
        CITADEL_WARLOCK -> "personage_citadel_warlock"
        FLAME_ELEMENT -> "personage_fire_element"
        EARTH_ELEMENT -> "personage_earth_element"
        BOSS_BLOOD_KNIGHT -> "personage_boss_blood_knight"
        else -> "personage_dead"
    }

    fun getPortrait() : String = when (this) {
        GLADIATOR -> "portrait_gladiator"
        GREEN_SLIME -> "portrait_slime"
        else -> "portrait_unknown"
    }
}