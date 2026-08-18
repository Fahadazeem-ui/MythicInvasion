package io.github.mindzard.mythicinvasion.application.society

import io.github.mindzard.mythicinvasion.concurrency.CoroutineEngine
import io.github.mindzard.mythicinvasion.infrastructure.paper.society.VillagerRelationshipCollector
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.bukkit.plugin.java.JavaPlugin

class VillagerRelationshipCoordinator(
    private val plugin: JavaPlugin,
    private val coroutineEngine: CoroutineEngine,
    private val collector: VillagerRelationshipCollector,
    private val stateStore: SocietyStateStore,
    private val relationshipStore: VillagerRelationshipStore,
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
                     * Native Villager reputation is Bukkit/Paper
                     * data, so collection remains on the main thread.
                     */
                    val relationships =
                        plugin.server.scheduler
                            .callSyncMethod(plugin) {

                                collector.collect(
                                    stateStore
                                        .current()
                                        .settlements
                                        .values
                                )
                            }
                            .get()

                    /*
                     * Store only plain Kotlin relationship objects
                     * outside the server thread.
                     */
                    relationshipStore.replaceAll(
                        relationships
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
