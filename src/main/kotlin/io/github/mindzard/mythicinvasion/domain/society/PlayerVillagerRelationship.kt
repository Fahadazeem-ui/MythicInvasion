package io.github.mindzard.mythicinvasion.domain.society

import java.util.UUID

data class PlayerVillagerRelationship(
    val villagerId: UUID,
    val playerId: UUID,
    val settlementId: String,
    val trading: Int = 0,
    val minorPositive: Int = 0,
    val majorPositive: Int = 0,
    val minorNegative: Int = 0,
    val majorNegative: Int = 0,
    val totalScore: Double = 0.0,
    val trust: Double = 0.0,
    val threat: Double = 0.0,
    val updatedAtMillis: Long = 0L
) {

    init {
        require(settlementId.isNotBlank()) {
            "Settlement ID cannot be blank."
        }

        require(trust in 0.0..1.0) {
            "Trust must be between 0.0 and 1.0."
        }

        require(threat in 0.0..1.0) {
            "Threat must be between 0.0 and 1.0."
        }
    }
}
