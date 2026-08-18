package io.github.mindzard.mythicinvasion.application.society

import io.github.mindzard.mythicinvasion.domain.society.SettlementState
import io.github.mindzard.mythicinvasion.domain.society.SettlementSocialProfile
import kotlin.math.max
import kotlin.math.min

data class PillagerSettlementTargetScore(
    val settlementId: String,
    val score: Double,
    val vulnerability: Double,
    val strategicValue: Double,
    val existingPressure: Double,
    val socialThreat: Double,
    val reason: String
)

class PillagerSettlementTargetingEngine {

    fun score(
        settlement: SettlementState,
        socialProfile: SettlementSocialProfile?,
        pillagerCountNearSettlement: Int
    ): PillagerSettlementTargetScore {

        val safetyVulnerability =
            (
                1.0 -
                    settlement.safetyLevel
                )
                .coerceIn(
                    0.0,
                    1.0
                )

        val guardProtection =
            if (
                settlement.population <= 0
            ) {
                0.0
            } else {

                val expectedGuards =
                    max(
                        1.0,
                        settlement.population * 0.20
                    )

                (
                    settlement.guardCount /
                        expectedGuards
                    )
                    .coerceIn(
                        0.0,
                        1.0
                    )
            }

        val guardWeakness =
            1.0 -
                guardProtection

        val foodWeakness =
            1.0 -
                settlement.foodLevel
                    .coerceIn(
                        0.0,
                        1.0
                    )

        val vulnerability =
            (
                safetyVulnerability * 0.50 +
                    guardWeakness * 0.30 +
                    foodWeakness * 0.20
                )
                .coerceIn(
                    0.0,
                    1.0
                )

        val populationValue =
            min(
                settlement.population /
                    40.0,
                1.0
            )

        val prosperityValue =
            settlement.prosperityLevel
                .coerceIn(
                    0.0,
                    1.0
                )

        val strategicValue =
            (
                populationValue * 0.55 +
                    prosperityValue * 0.45
                )
                .coerceIn(
                    0.0,
                    1.0
                )

        val existingPressure =
            min(
                pillagerCountNearSettlement /
                    12.0,
                1.0
            )

        val socialThreat =
            (
                socialProfile
                    ?.averageThreat
                    ?: 0.0
            )
                .coerceIn(
                    0.0,
                    1.0
                )

        val finalScore =
            (
                vulnerability * 0.40 +
                    strategicValue * 0.30 +
                    existingPressure * 0.15 +
                    socialThreat * 0.15
                )
                .coerceIn(
                    0.0,
                    1.0
                )

        val reason =
            when {

                vulnerability >= 0.75 &&
                    strategicValue >= 0.50 ->
                    "Highly vulnerable and strategically valuable."

                vulnerability >= 0.75 ->
                    "Settlement defenses are weak."

                strategicValue >= 0.75 ->
                    "Settlement has high strategic value."

                existingPressure >= 0.70 ->
                    "Pillager presence is already concentrated nearby."

                socialThreat >= 0.70 ->
                    "Settlement has elevated social hostility."

                else ->
                    "Balanced strategic target."
            }

        return PillagerSettlementTargetScore(
            settlementId =
                settlement.settlementId,

            score =
                finalScore,

            vulnerability =
                vulnerability,

            strategicValue =
                strategicValue,

            existingPressure =
                existingPressure,

            socialThreat =
                socialThreat,

            reason =
                reason
        )
    }
}
