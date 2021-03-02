package com.game7th.metagame.unit

enum class UnitType(val bodyWeight: Int, val spiritWeight: Int, val mindWeight: Int) {
    GLADIATOR(3,2,1),
    POISON_ARCHER(1,3,2),
    MACHINE_GUNNER(2, 1, 3),

    GREEN_SLIME(0,0,0),
    PURPLE_SLIME(0,0,0),
    SLIME_MOTHER(0,0,0),
    SLIME_FATHER(0,0,0),
    CITADEL_WARLOCK(0,0,0),
    FLAME_ELEMENT(0,0,0),
    EARTH_ELEMENT(0,0,0),
    BOSS_BLOOD_KNIGHT(0,0,0),

    UNKNOWN(0,0,0);

    fun getSkin(): String = when (this) {
        GLADIATOR -> "p_gladiator"
        POISON_ARCHER -> "personage_ranger"
        GREEN_SLIME -> "personage_slime"
        SLIME_MOTHER -> "personage_mother_slime"
        SLIME_FATHER -> "personage_slime_father"
        PURPLE_SLIME -> "personage_red_slime"
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
        PURPLE_SLIME -> "portrait_slime_red"
        SLIME_MOTHER -> "portrait_slime_mother"
        SLIME_FATHER -> "portrait_slime_father"
        else -> "portrait_unknown"
    }
}