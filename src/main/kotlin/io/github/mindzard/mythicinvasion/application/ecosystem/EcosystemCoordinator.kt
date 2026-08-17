package io.github.mindzard.mythicinvasion.application.ecosystem

import io.github.mindzard.mythicinvasion.concurrency.CoroutineEngine
import io.github.mindzard.mythicinvasion.infrastructure.paper.ecosystem.PlayerSnapshotCollector
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.bukkit.plugin.java.JavaPlugin

class EcosystemCoordinator(
    private val plugin: JavaPlugin,
    private val coroutineEngine: CoroutineEngine,
    private val snapshotCollector: PlayerSnapshotCollector,
    private val ecosystemEngine: EcosystemEngine,
    private val updateIntervalMillisProvider: () -> Long
) {

    private var simulationJob: Job? = null

    fun start() {
        if (simulationJob != null) {
            return
        }

        simulationJob = coroutineEngine.scope.launch {

            while (isActive) {

                /*
                 * We must obtain Bukkit data on the server thread.
                 *
                 * The collector itself therefore cannot be called
                 * directly from this coroutine.
                 */

                val snapshot = plugin.server.scheduler
                    .callSyncMethod(
                        plugin
                    ) {
                        snapshotCollector.collect()
                    }
                    .get()
                    .let { players ->
                        io.github.mindzard.mythicinvasion.domain.ecosystem.EcosystemSnapshot(
                            timestampMillis = System.currentTimeMillis(),
                            players = players
                        )
                    }

                val state = ecosystemEngine.process(snapshot)

                if (plugin.server.isPrimaryThread) {
                    plugin.logger.info(
                        "Ecosystem state: players=${state.currentPlayerCount}, " +
                            "threat=${"%.3f".format(state.globalThreatLevel)}"
                    )
                } else {
                    plugin.logger.info(
                        "Ecosystem state: players=${state.currentPlayerCount}, " +
                            "threat=${"%.3f".format(state.globalThreatLevel)}"
                    )
                }

                delay(
                    updateIntervalMillisProvider()
                )
            }
        }
    }

    fun stop() {
        simulationJob?.cancel()
        simulationJob = null
    }
}
