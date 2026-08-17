package io.github.mindzard.mythicinvasion.domain.player

import java.util.UUID

data class PlayerSnapshot(
    val playerId: UUID,
    val name: String,
    val worldName: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val health: Double,
    val level: Int,
    val isSneaking: Boolean,
    val isSprinting: Boolean
)
