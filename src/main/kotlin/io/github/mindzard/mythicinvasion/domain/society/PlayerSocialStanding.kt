package io.github.mindzard.mythicinvasion.domain.society

import java.util.UUID

enum class SocialStanding {
    TRUSTED,
    NEUTRAL,
    DISTRUSTED,
    HOSTILE
}

data class PlayerSocialStanding(
    val settlementId: String,
    val playerId: UUID,
    val trust: Double,
    val threat: Double,
    val standing: SocialStanding,
    val updatedAtMillis: Long
)
