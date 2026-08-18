package io.github.mindzard.mythicinvasion.application.ai

import io.github.mindzard.mythicinvasion.domain.ai.AdaptiveTargetScore
import io.github.mindzard.mythicinvasion.domain.intelligence.PlayerIntelligenceProfile
import java.util.UUID

class AdaptiveTargetingEngine {

    fun score(
        playerId: UUID,
        distanceSquared: Double,
        maximumDistance: Double,
        intelligenceProfile: PlayerIntelligenceProfile?,
        socialThreat: Double,
        activityLevel: Double,
        strategicPressure: Double
    ): AdaptiveTargetScore {

        val maximumDistanceSquared =
            maximumDistance *
                maximumDistance

        val proximityScore =
            if (
                maximumDistanceSquared <= 0.0
            ) {
                0.0
            } else {
                (
                    1.0 -
                        (
                            distanceSquared /
                                maximumDistanceSquared
                            )
                    )
                    .coerceIn(
                        0.0,
                        1.0
                    )
            }

        val combatScore =
            intelligenceProfile
                ?.score(
                    io.github.mindzard.mythicinvasion
                        .domain.intelligence.BehaviourArchetype.WARRIOR
                )
                ?: 0.0

        val normalizedSocialThreat =
            socialThreat.coerceIn(
                0.0,
                1.0
            )

        val normalizedActivity =
            activityLevel.coerceIn(
                0.0,
                1.0
            )

        val normalizedPressure =
            strategicPressure.coerceIn(
                0.0,
                1.0
            )

        val baseScore =
            (
                proximityScore * 0.35 +
                    combatScore * 0.30 +
                    normalizedSocialThreat * 0.25 +
                    normalizedActivity * 0.10
                )
                .coerceIn(
                    0.0,
                    1.0
                )

        val finalScore =
            (
                baseScore +
                    normalizedPressure * 0.15
                )
                .coerceIn(
                    0.0,
                    1.0
                )

        return AdaptiveTargetScore(
            playerId =
                playerId,

            score =
                finalScore,

            proximityScore =
                proximityScore,

            combatScore =
                combatScore,

            socialThreatScore =
                normalizedSocialThreat,

            activityScore =
                normalizedActivity
        )
    }
}
