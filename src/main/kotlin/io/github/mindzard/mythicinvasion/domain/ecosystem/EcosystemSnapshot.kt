package io.github.mindzard.mythicinvasion.domain.ecosystem

import io.github.mindzard.mythicinvasion.domain.player.PlayerSnapshot

data class EcosystemSnapshot(
    val timestampMillis: Long,
    val players: List<PlayerSnapshot>
) {
    val playerCount: Int
        get() = players.size
}
