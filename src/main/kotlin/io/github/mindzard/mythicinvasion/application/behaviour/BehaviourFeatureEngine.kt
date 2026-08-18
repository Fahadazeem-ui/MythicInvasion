package io.github.mindzard.mythicinvasion.application.behaviour

import io.github.mindzard.mythicinvasion.domain.behaviour.BehaviourFeatures
import io.github.mindzard.mythicinvasion.domain.behaviour.PlayerBehaviourProfile

/**
 * Converts raw behaviour counters into normalized behavioural signals.
 *
 * This engine is intentionally deterministic and side-effect free.
 * It does not use Bukkit API, network requests, databases, or AI.
 */
class BehaviourFeatureEngine {

    fun calculate(
        profile: PlayerBehaviourProfile
    ): BehaviourFeatures {

        val totalEvents =
            profile.totalEvents.toDouble().coerceAtLeast(1.0)

        val actionEvents =
            (
                profile.blocksBroken +
                    profile.blocksPlaced +
                    profile.combatActions
                ).toDouble()
                .coerceAtLeast(1.0)

        val miningScore =
            normalize(
                profile.blocksBroken.toDouble() / actionEvents
            )

        val buildingScore =
            normalize(
                profile.blocksPlaced.toDouble() / actionEvents
            )

        val combatScore =
            normalize(
                profile.combatActions.toDouble() / actionEvents
            )

        val movementScore =
            normalize(
                profile.movements.toDouble() / totalEvents
            )

        /*
         * Activity is deliberately based on total observations rather
         * than a behaviour category.
         *
         * 5,000 observed events represents a highly active profile
         * for this first-generation heuristic model.
         *
         * This threshold will later become configurable and eventually
         * time-window based.
         */
        val activityScore =
            normalize(
                profile.totalEvents.toDouble() / 5_000.0
            )

        return BehaviourFeatures(
            miningTendency = miningScore,
            buildingTendency = buildingScore,
            combatTendency = combatScore,
            movementTendency = movementScore,
            activityLevel = activityScore
        )
    }

    private fun normalize(value: Double): Double {
        return value
            .coerceIn(0.0, 1.0)
    }
}
