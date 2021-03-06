package com.game7th.battle

data class DamageVector(
        val physical: Int,
        val magical: Int,
        val chaos: Int
) {
    override fun toString(): String = "($physical, $magical, $chaos)"

    fun totalDamage() = physical + magical + chaos
    fun multiply(koef: Float) = DamageVector(
            (physical * koef).toInt(),
            (magical * koef).toInt(),
            (chaos * koef).toInt()
    )
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
) {
    override fun toString(): String = "$damage {$armorDeplete/$resistDeplete} status=$status"

    fun totalDamage() = damage.totalDamage() + armorDeplete + resistDeplete
}