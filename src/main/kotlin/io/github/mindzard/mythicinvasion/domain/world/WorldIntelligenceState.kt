package io.github.mindzard.mythicinvasion.domain.world

data class WorldIntelligenceState(
    val lastUpdatedMillis: Long = 0L,
    val totalPlayers: Int = 0,
    val totalVillagers: Int = 0,
    val totalPillagers: Int = 0,
    val totalHostileMobs: Int = 0,
    val totalPassiveAnimals: Int = 0,
    val totalWorlds: Int = 0,
    val globalActivityLevel: Double = 0.0,
    val globalThreatLevel: Double = 0.0
)
