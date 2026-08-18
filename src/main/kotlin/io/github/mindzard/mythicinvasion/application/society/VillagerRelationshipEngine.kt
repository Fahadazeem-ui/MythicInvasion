package io.github.mindzard.mythicinvasion.application.society

import com.destroystokyo.paper.entity.villager.Reputation
import com.destroystokyo.paper.entity.villager.ReputationType
import io.github.mindzard.mythicinvasion.domain.society.PlayerVillagerRelationship
import java.util.UUID

class VillagerRelationshipEngine {

    fun calculate(
        villagerId: UUID,
        playerId: UUID,
        settlementId: String,
        reputation: Reputation,
        updatedAtMillis: Long
    ): PlayerVillagerRelationship {

        val trading =
            reputation.getReputation(
                ReputationType.TRADING
            )

        val minorPositive =
            reputation.getReputation(
                ReputationType.MINOR_POSITIVE
            )

        val majorPositive =
            reputation.getReputation(
                ReputationType.MAJOR_POSITIVE
            )

        val minorNegative =
            reputation.getReputation(
                ReputationType.MINOR_NEGATIVE
            )

        val majorNegative =
            reputation.getReputation(
                ReputationType.MAJOR_NEGATIVE
            )

        /*
         * Positive values contribute toward trust.
         * Negative values contribute toward threat.
         *
         * Major negative reputation receives the strongest weight
         * because it represents a serious hostile history.
         */
        val positiveScore =
            (
                trading * 1.0 +
                    minorPositive * 1.5 +
                    majorPositive * 3.0
                )

        val negativeScore =
            (
                minorNegative * 1.5 +
                    majorNegative * 3.0
                )

        val totalScore =
            (
                positiveScore -
                    negativeScore
                )
                .coerceIn(
                    -100.0,
                    100.0
                )

        val trust =
            calculateTrust(
                totalScore
            )

        val threat =
            calculateThreat(
                totalScore,
                negativeScore
            )

        return PlayerVillagerRelationship(
            villagerId =
                villagerId,

            playerId =
                playerId,

            settlementId =
                settlementId,

            trading =
                trading,

            minorPositive =
                minorPositive,

            majorPositive =
                majorPositive,

            minorNegative =
                minorNegative,

            majorNegative =
                majorNegative,

            totalScore =
                totalScore,

            trust =
                trust,

            threat =
                threat,

            updatedAtMillis =
                updatedAtMillis
        )
    }

    private fun calculateTrust(
        totalScore: Double
    ): Double {

        return when {
            totalScore <= 0.0 -> {
                0.0
            }

            totalScore >= 100.0 -> {
                1.0
            }

            else -> {
                (
                    totalScore / 100.0
                )
                    .coerceIn(
                        0.0,
                        1.0
                    )
            }
        }
    }

    private fun calculateThreat(
        totalScore: Double,
        negativeScore: Double
    ): Double {

        val negativeSignal =
            (
                negativeScore / 100.0
            )
                .coerceIn(
                    0.0,
                    1.0
                )

        val hostileBias =
            when {
                totalScore < -50.0 -> 0.35
                totalScore < -20.0 -> 0.20
                totalScore < 0.0 -> 0.10
                else -> 0.0
            }

        return (
            negativeSignal +
                hostileBias
            )
            .coerceIn(
                0.0,
                1.0
            )
    }
}
