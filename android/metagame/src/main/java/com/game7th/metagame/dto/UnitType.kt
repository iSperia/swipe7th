package com.game7th.metagame.dto

enum class UnitType(val bodyWeight: Int, val spiritWeight: Int, val mindWeight: Int) {
    GLADIATOR(3,2,1),
    POISON_ARCHER(1,3,2),
    FREEZE_MAGE(1, 2, 3),
    MACHINE_GUNNER(2, 1, 3),
    PRINCE(2,2,2),

    GREEN_SLIME(0,0,0),
    SLIME_PURPLE(0,0,0),
    SLIME_MOTHER(0,0,0),
    SLIME_FATHER(0,0,0),
    SLIME_BOSS(0,0,0),
    SLIME_ARMORED(0,0,0),
    BHASTUSE_JOLLY(0,0,0),

    DRYAD(0,0,0),
    DRYAD_QUEEN(0,0,0),

    UNKNOWN(0,0,0);

    fun getSkin(): String = when (this) {
        GLADIATOR -> "personage_gladiator"
        POISON_ARCHER -> "poison_archer"
        FREEZE_MAGE -> "freeze_mage"
        GREEN_SLIME -> "slime"
        SLIME_MOTHER -> "slime_mother"
        SLIME_FATHER -> "slime_father"
        SLIME_PURPLE -> "slime_red"
        SLIME_BOSS -> "slime_boss"
        DRYAD -> "dryad"
        DRYAD_QUEEN -> "dryad_queen"
        SLIME_ARMORED -> "slime_armored"
        BHASTUSE_JOLLY -> "bhastuse_jolly"
        PRINCE -> "prince"
        else -> "personage_dead"
    }

    fun getPortrait() : String = when (this) {
        GLADIATOR -> "portrait_gladiator"
        GREEN_SLIME -> "portrait_slime"
        FREEZE_MAGE -> "freeze_mage"
        SLIME_PURPLE -> "portrait_slime_red"
        SLIME_MOTHER -> "portrait_slime_mother"
        SLIME_FATHER -> "portrait_slime_father"
        POISON_ARCHER -> "portrait_poison_archer"
        SLIME_BOSS -> "slime_boss"
        DRYAD -> "dryad"
        DRYAD_QUEEN -> "dryad_queen"
        SLIME_ARMORED -> "slime_armored"
        BHASTUSE_JOLLY -> "bhastuse_jolly"
        PRINCE -> "prince"
        else -> "portrait_unknown"
    }
}