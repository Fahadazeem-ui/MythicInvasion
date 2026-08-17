package io.github.mindzard.mythicinvasion.application.ecosystem

import io.github.mindzard.mythicinvasion.domain.ecosystem.EcosystemSnapshot
import io.github.mindzard.mythicinvasion.domain.ecosystem.EcosystemState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.bukkit.plugin.java.JavaPlugin
import kotlin.math.min

class EcosystemEngine(
    private val plugin: JavaPlugin
) {

    private val stateMutex = Mutex()

    private var state = EcosystemState()

    suspend fun process(snapshot: EcosystemSnapshot): EcosystemState {
        val calculatedThreat = calculateGlobalThreat(snapshot)

        val newState = EcosystemState(
            lastUpdateMillis = snapshot.timestampMillis,
            currentPlayerCount = snapshot.playerCount,
            globalThreatLevel = calculatedThreat
        )

        stateMutex.withLock {
            state = newState
        }

        return newState
    }

    suspend fun currentState(): EcosystemState {
        return stateMutex.withLock {
            state
        }
    }

    private fun calculateGlobalThreat(
        snapshot: EcosystemSnapshot
    ): Double {

        /*
         * This is deliberately a tiny first-generation model.
         *
         * We are NOT pretending this is "AI".
         *
         * It is simply deterministic simulation logic which will
         * later become one input into our actual intelligence system.
         */
        val playerFactor = min(
            snapshot.playerCount / 10.0,
            1.0
        )

        return playerFactor
    }
}
