package io.github.mindzard.mythicinvasion.domain.society

import java.util.UUID

data class SettlementSocialProfile(
    val settlementId: String,
    val trustedPlayers: Map<UUID, Double> = emptyMap(),
    val neutralPlayers: Map<UUID, Double> = emptyMap(),
    val hostilePlayers: Map<UUID, Double> = emptyMap(),
    val averageTrust: Double = 0.0,
    val averageThreat: Double = 0.0,
    val updatedAtMillis: Long = 0L
) {

    val knownPlayerCount: Int
        get() =
            trustedPlayers.size +
                neutralPlayers.size +
                hostilePlayers.size
}
