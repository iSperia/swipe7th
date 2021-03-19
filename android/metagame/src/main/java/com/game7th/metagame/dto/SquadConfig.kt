package com.game7th.metagame.dto

import com.game7th.metagame.dto.UnitConfig

data class SquadConfig(
        val name: String,
        val units: List<UnitConfig>
)