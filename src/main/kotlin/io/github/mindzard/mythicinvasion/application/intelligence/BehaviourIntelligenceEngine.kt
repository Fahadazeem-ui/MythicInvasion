package io.github.mindzard.mythicinvasion.application.intelligence

import io.github.mindzard.mythicinvasion.domain.behaviour.PlayerBehaviourProfile
import io.github.mindzard.mythicinvasion.domain.intelligence.BehaviourArchetype
import io.github.mindzard.mythicinvasion.domain.intelligence.PlayerIntelligenceProfile
import kotlin.math.exp

class BehaviourIntelligenceEngine {

    companion object {
        private const val SMOOTHING_FACTOR = 0.25

        private const val CONFIDENCE_SAMPLE_SCALE = 500.0
    }

    fun calculate(
        profile: PlayerBehaviourProfile,
        previous: PlayerIntelligenceProfile?
    ): PlayerIntelligenceProfile {

        val features = profile.features

        /*
         * These are deliberately transparent heuristics.
         *
         * Later, as richer data becomes available, these formulas
         * can be replaced by more sophisticated models without
         * changing the rest of the architecture.
         */

        val miner =
            weightedScore(
                features.miningTendency to 0.70,
                features.activityLevel to 0.15,
                features.movementTendency to 0.15
            )

        val builder =
            weightedScore(
                features.buildingTendency to 0.75,
                features.activityLevel to 0.15,
                features.movementTendency to 0.10
            )

        val warrior =
            weightedScore(
                features.combatTendency to 0.80,
                features.activityLevel to 0.10,
                features.movementTendency to 0.10
            )

        val explorer =
            weightedScore(
                features.movementTendency to 0.65,
                features.activityLevel to 0.25,
                features.combatTendency to 0.10
            )

        val rawScores =
            mapOf(
                BehaviourArchetype.MINER to miner,
                BehaviourArchetype.BUILDER to builder,
                BehaviourArchetype.WARRIOR to warrior,
                BehaviourArchetype.EXPLORER to explorer
            )

        val smoothedScores =
            smooth(
                rawScores,
                previous?.archetypeScores
            )

        val confidence =
            calculateConfidence(
                profile.totalEvents
            )

        val dominant =
            smoothedScores.maxByOrNull { it.value }
                ?.key

        return PlayerIntelligenceProfile(
            playerId = profile.playerId,
            archetypeScores = smoothedScores,
            confidence = confidence,
            dominantArchetype = dominant,
            sampleCount = profile.totalEvents,
            lastUpdatedMillis = System.currentTimeMillis()
        )
    }

    private fun weightedScore(
        vararg values: Pair<Double, Double>
    ): Double {

        val totalWeight =
            values.sumOf { it.second }

        if (totalWeight <= 0.0) {
            return 0.0
        }

        val weightedValue =
            values.sumOf { (value, weight) ->
                value.coerceIn(0.0, 1.0) * weight
            }

        return (
            weightedValue / totalWeight
        ).coerceIn(0.0, 1.0)
    }

    private fun smooth(
        current: Map<BehaviourArchetype, Double>,
        previous: Map<BehaviourArchetype, Double>?
    ): Map<BehaviourArchetype, Double> {

        if (previous.isNullOrEmpty()) {
            return current
        }

        return current.mapValues { (archetype, currentValue) ->

            val previousValue =
                previous[archetype] ?: currentValue

            (
                previousValue +
                    (
                        currentValue -
                            previousValue
                    ) * SMOOTHING_FACTOR
                ).coerceIn(0.0, 1.0)
        }
    }

    private fun calculateConfidence(
        sampleCount: Long
    ): Double {

        if (sampleCount <= 0L) {
            return 0.0
        }

        /*
         * Confidence asymptotically approaches 1 instead of suddenly
         * jumping from "untrusted" to "trusted".
         */
        return (
            1.0 -
                exp(
                    -sampleCount.toDouble() /
                        CONFIDENCE_SAMPLE_SCALE
                )
            ).coerceIn(0.0, 1.0)
    }
}
