package io.github.mindzard.mythicinvasion.application.society

import io.github.mindzard.mythicinvasion.domain.society.FactionRelation
import io.github.mindzard.mythicinvasion.domain.society.FactionState

class FactionRelationService {

    fun updateRelation(
        source: FactionState,
        targetFactionId: String,
        valueDelta: Double,
        trustDelta: Double,
        fearDelta: Double,
        respectDelta: Double,
        nowMillis: Long
    ): FactionState {

        val existing =
            source.relations[targetFactionId]
                ?: FactionRelation(
                    sourceFactionId =
                        source.factionId,

                    targetFactionId =
                        targetFactionId
                )

        val updated =
            existing.copy(
                value =
                    (
                        existing.value +
                            valueDelta
                        ).coerceIn(
                            -1.0,
                            1.0
                        ),

                trust =
                    (
                        existing.trust +
                            trustDelta
                        ).coerceIn(
                            0.0,
                            1.0
                        ),

                fear =
                    (
                        existing.fear +
                            fearDelta
                        ).coerceIn(
                            0.0,
                            1.0
                        ),

                respect =
                    (
                        existing.respect +
                            respectDelta
                        ).coerceIn(
                            0.0,
                            1.0
                        ),

                lastUpdatedMillis =
                    nowMillis
            )

        return source.copy(
            relations =
                source.relations +
                    (
                        targetFactionId to
                            updated
                        ),

            lastUpdatedMillis =
                nowMillis
        )
    }
}
