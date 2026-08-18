package io.github.mindzard.mythicinvasion.application.world

import io.github.mindzard.mythicinvasion.domain.world.WorldIntelligenceState
import io.github.mindzard.mythicinvasion.domain.world.WorldSnapshot
import kotlin.math.min

class WorldIntelligenceEngine {

    fun calculate(
        snapshot: WorldSnapshot
    ): WorldIntelligenceState {

        val totalEntities =
            (
                snapshot.totalVillagers +
                    snapshot.totalPillagers +
                    snapshot.totalHostileMobs +
                    snapshot.totalPassiveAnimals
                ).coerceAtLeast(1)

        val hostileRatio =
            snapshot.totalHostileMobs
                .toDouble() /
                totalEntities.toDouble()

        val populationActivity =
            min(
                (
                    snapshot.totalPlayers * 0.10 +
                        snapshot.totalVillagers * 0.02 +
                        snapshot.totalPillagers * 0.04 +
                        snapshot.totalHostileMobs * 0.01
                    ),
                1.0
            )

        val threatLevel =
            (
                hostileRatio * 0.60 +
                    min(
                        snapshot.totalPillagers / 50.0,
                        1.0
                    ) * 0.25 +
                    if (snapshot.isDay) {
                        0.0
                    } else {
                        0.15
                    }
                )
                .coerceIn(0.0, 1.0)

        return WorldIntelligenceState(
            lastUpdatedMillis =
                snapshot.timestampMillis,
            totalPlayers =
                snapshot.totalPlayers,
            totalVillagers =
                snapshot.totalVillagers,
            totalPillagers =
                snapshot.totalPillagers,
            totalHostileMobs =
                snapshot.totalHostileMobs,
            totalPassiveAnimals =
                snapshot.totalPassiveAnimals,
            totalWorlds =
                snapshot.populations.size,
            globalActivityLevel =
                populationActivity,
            globalThreatLevel =
                threatLevel
        )
    }
}
