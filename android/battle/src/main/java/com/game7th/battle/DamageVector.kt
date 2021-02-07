package com.game7th.battle

data class DamageVector(
        val physical: Int,
        val magical: Int,
        val chaos: Int
) {
    fun totalDamage() = physical + magical + chaos
}

enum class DamageProcessStatus{
    DAMAGE_DEALT,
    DAMAGE_EVADED
}

data class DamageProcessResult(
        val damage: DamageVector,
        val armorDeplete: Int,
        val resistDeplete: Int,
        val status: DamageProcessStatus
)