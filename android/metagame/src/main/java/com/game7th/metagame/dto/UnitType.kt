package com.game7th.metagame.dto

enum class UnitType(val bodyWeight: Int, val spiritWeight: Int, val mindWeight: Int) {
    prince(2,2,2),
    stomper(0,0,0),
    UNKNOWN(0,0,0);

    fun getSkin(): String = when (this) {
        prince -> "prince"
        stomper -> "stomper"
        else -> "personage_dead"
    }

    fun getPortrait() : String = when (this) {
        prince -> "prince"
        stomper -> "stomper"
        else -> "portrait_unknown"
    }
}