package io.github.mindzard.mythicinvasion.domain.ai

import java.util.UUID

data class AdaptiveTargetScore(
    val playerId: UUID,
    val score: Double,
    val proximityScore: Double,
    val combatScore: Double,
    val socialThreatScore: Double,
    val activityScore: Double
) {

    init {
        require(score in 0.0..1.0) {
            "Adaptive target score must be between 0.0 and 1.0."
        }

        require(proximityScore in 0.0..1.0) {
            "Proximity score must be between 0.0 and 1.0."
        }

        require(combatScore in 0.0..1.0) {
            "Combat score must be between 0.0 and 1.0."
        }

        require(socialThreatScore in 0.0..1.0) {
            "Social threat score must be between 0.0 and 1.0."
        }

        require(activityScore in 0.0..1.0) {
            "Activity score must be between 0.0 and 1.0."
        }
    }
}
