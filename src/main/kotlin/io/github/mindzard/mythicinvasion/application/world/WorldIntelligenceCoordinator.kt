package io.github.mindzard.mythicinvasion.application.world

import io.github.mindzard.mythicinvasion.concurrency.CoroutineEngine
import io.github.mindzard.mythicinvasion.infrastructure.paper.world.WorldSnapshotCollector
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.bukkit.plugin.java.JavaPlugin

class WorldIntelligenceCoordinator(
    private val plugin: JavaPlugin,
    private val coroutineEngine: CoroutineEngine,
    private val collector: WorldSnapshotCollector,
    private val engine: WorldIntelligenceEngine,
    private val stateStore: WorldStateStore,
    private val updateIntervalMillis: () -> Long
) {

    private var job: Job? = null

    fun start() {

        if (job != null) {
            return
        }

        job =
            coroutineEngine.scope.launch {

                while (isActive) {

                    /*
                     * Bukkit/Paper world access must happen on
                     * the server thread.
                     */
                    val snapshot =
                        plugin.server.scheduler
                            .callSyncMethod(plugin) {
                                collector.collect()
                            }
                            .get()

                    /*
                     * Snapshot is plain Kotlin data from here.
                     *
                     * Everything below can safely run asynchronously.
                     */
                    val newState =
                        engine.calculate(
                            snapshot
                        )

                    stateStore.update(
                        newState
                    )

                    delay(
                        updateIntervalMillis()
                    )
                }
            }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
