package com.game7th.metagame.dto

enum class UnitType(val bodyWeight: Int, val spiritWeight: Int, val mindWeight: Int) {
    GLADIATOR(3,2,1),
    POISON_ARCHER(1,3,2),
    MACHINE_GUNNER(2, 1, 3),

    GREEN_SLIME(0,0,0),
    PURPLE_SLIME(0,0,0),
    SLIME_MOTHER(0,0,0),
    SLIME_FATHER(0,0,0),
    SLIME_BOSS(0,0,0),

    DRYAD(0,0,0),

    UNKNOWN(0,0,0);

    fun getSkin(): String = when (this) {
        GLADIATOR -> "personage_gladiator"
        POISON_ARCHER -> "poison_archer"
        GREEN_SLIME -> "slime"
        SLIME_MOTHER -> "slime_mother"
        SLIME_FATHER -> "slime_father"
        PURPLE_SLIME -> "slime_red"
        SLIME_BOSS -> "slime_boss"
        DRYAD -> "dryad"
        else -> "personage_dead"
    }

    fun getPortrait() : String = when (this) {
        GLADIATOR -> "portrait_gladiator"
        GREEN_SLIME -> "portrait_slime"
        PURPLE_SLIME -> "portrait_slime_red"
        SLIME_MOTHER -> "portrait_slime_mother"
        SLIME_FATHER -> "portrait_slime_father"
        POISON_ARCHER -> "portrait_poison_archer"
        SLIME_BOSS -> "slime_boss"
        DRYAD -> "dryad"
        else -> "portrait_unknown"
    }
}