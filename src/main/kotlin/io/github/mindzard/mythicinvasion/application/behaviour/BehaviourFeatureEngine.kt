package io.github.mindzard.mythicinvasion.application.behaviour

import io.github.mindzard.mythicinvasion.domain.behaviour.BehaviourFeatures
import io.github.mindzard.mythicinvasion.domain.behaviour.PlayerBehaviourProfile

/**
 * Converts time-weighted behaviour observations into normalized
 * behavioural signals.
 *
 * This is still a deterministic heuristic engine.
 * It is not machine learning and does not contact any AI provider.
 */
class BehaviourFeatureEngine {

    fun calculate(
        profile: PlayerBehaviourProfile
    ): BehaviourFeatures {

        val weightedActionTotal =
            (
                profile.weightedBlocksBroken +
                    profile.weightedBlocksPlaced +
                    profile.weightedCombatActions
                )
                .coerceAtLeast(0.000001)

        val miningScore =
            normalize(
                profile.weightedBlocksBroken /
                    weightedActionTotal
            )

        val buildingScore =
            normalize(
                profile.weightedBlocksPlaced /
                    weightedActionTotal
            )

        val combatScore =
            normalize(
                profile.weightedCombatActions /
                    weightedActionTotal
            )

        /*
         * Movement is independent from the action distribution.
         *
         * A player who is moving consistently has a higher movement
         * signal, while old movement naturally decays over time.
         */
        val movementScore =
            normalize(
                profile.weightedMovements /
                    20.0
            )

        /*
         * Overall activity represents how much recent meaningful
         * behaviour remains in the weighted model.
         */
        val activityScore =
            normalize(
                (
                    profile.weightedMovements +
                        profile.weightedBlocksBroken +
                        profile.weightedBlocksPlaced +
                        profile.weightedCombatActions
                    ) /
                    100.0
            )

        return BehaviourFeatures(
            miningTendency = miningScore,
            buildingTendency = buildingScore,
            combatTendency = combatScore,
            movementTendency = movementScore,
            activityLevel = activityScore
        )
    }

    private fun normalize(
        value: Double
    ): Double {

        return value.coerceIn(
            0.0,
            1.0
        )
    }
}
