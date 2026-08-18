package io.github.mindzard.mythicinvasion.application.society

import io.github.mindzard.mythicinvasion.domain.society.SettlementObservation
import io.github.mindzard.mythicinvasion.domain.society.SettlementState

class SettlementObservationEngine {

    fun convert(
        observation: SettlementObservation
    ): SettlementState {

        val population =
            observation.villagerCount

        val safetyLevel =
            calculateSafety(
                villagerCount =
                    observation.villagerCount,

                ironGolemCount =
                    observation.ironGolemCount
            )

        val prosperityLevel =
            calculateProsperity(
                villagerCount =
                    observation.villagerCount,

                ironGolemCount =
                    observation.ironGolemCount
            )

        return SettlementState(
            settlementId =
                observation.settlementId,

            name =
                observation.name,

            worldName =
                observation.worldName,

            centerX =
                observation.centerX,

            centerY =
                observation.centerY,

            centerZ =
                observation.centerZ,

            radius =
                observation.radius,

            population =
                population,

            guardCount =
                observation.ironGolemCount,

            foodLevel =
                1.0,

            safetyLevel =
                safetyLevel,

            prosperityLevel =
                prosperityLevel,

            ownerFactionId =
                "villagers",

            lastUpdatedMillis =
                observation.lastUpdatedMillis
        )
    }

    private fun calculateSafety(
        villagerCount: Int,
        ironGolemCount: Int
    ): Double {

        if (
            villagerCount <= 0
        ) {
            return 0.0
        }

        val guardCoverage =
            ironGolemCount.toDouble() /
                villagerCount.toDouble()

        return (
            0.35 +
                guardCoverage * 0.65
            )
            .coerceIn(
                0.0,
                1.0
            )
    }

    private fun calculateProsperity(
        villagerCount: Int,
        ironGolemCount: Int
    ): Double {

        val populationSignal =
            (
                villagerCount / 20.0
            ).coerceIn(
                0.0,
                1.0
            )

        val protectionSignal =
            (
                ironGolemCount / 3.0
            ).coerceIn(
                0.0,
                1.0
            )

        return (
            populationSignal * 0.75 +
                protectionSignal * 0.25
            )
            .coerceIn(
                0.0,
                1.0
            )
    }
}
