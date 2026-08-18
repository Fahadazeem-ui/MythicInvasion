package io.github.mindzard.mythicinvasion.application.society

import io.github.mindzard.mythicinvasion.concurrency.CoroutineEngine
import io.github.mindzard.mythicinvasion.infrastructure.paper.society.VillagerObservationCollector
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.bukkit.plugin.java.JavaPlugin

class VillagerSocietyCoordinator(
    private val plugin: JavaPlugin,
    private val coroutineEngine: CoroutineEngine,
    private val collector: VillagerObservationCollector,
    private val engine: VillagerSocietyEngine,
    private val stateStore: SocietyStateStore,
    private val societyStore: VillagerSocietyStore,
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
                     * Snapshot only Minecraft objects on the
                     * server thread.
                     */
                    val villagers =
                        plugin.server.scheduler
                            .callSyncMethod(plugin) {
                                collector.collect(
                                    stateStore.current()
                                        .settlements
                                        .values
                                )
                            }
                            .get()

                    /*
                     * From here onward we operate exclusively
                     * on immutable Kotlin data.
                     */
                    val profiles =
                        engine.buildProfiles(
                            villagers
                        )

                    societyStore.replaceAll(
                        profiles
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
