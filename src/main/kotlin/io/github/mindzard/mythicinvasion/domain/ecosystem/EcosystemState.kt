package io.github.mindzard.mythicinvasion.domain.ecosystem

data class EcosystemState(
    val lastUpdateMillis: Long = 0L,
    val currentPlayerCount: Int = 0,
    val globalThreatLevel: Double = 0.0
)
