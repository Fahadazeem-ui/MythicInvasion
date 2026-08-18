package io.github.mindzard.mythicinvasion.application.society

import io.github.mindzard.mythicinvasion.concurrency.CoroutineEngine
import io.github.mindzard.mythicinvasion.infrastructure.paper.society.SettlementObservationCollector
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.bukkit.plugin.java.JavaPlugin

class SettlementObservationCoordinator(
    private val plugin: JavaPlugin,
    private val coroutineEngine: CoroutineEngine,
    private val collector: SettlementObservationCollector,
    private val engine: SettlementObservationEngine,
    private val stateStore: SocietyStateStore,
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
                     * Minecraft entity access happens on the
                     * main server thread.
                     */
                    val observations =
                        plugin.server.scheduler
                            .callSyncMethod(plugin) {
                                collector.collect()
                            }
                            .get()

                    /*
                     * Everything below operates only on our own
                     * plain Kotlin data.
                     */
                    val currentState =
                        stateStore.current()

                    val newSettlements =
                        observations
                            .associate {
                                observation ->

                                val settlement =
                                    engine.convert(
                                        observation
                                    )

                                settlement.settlementId to
                                    settlement
                            }

                    val mergedState =
                        currentState.copy(
                            lastUpdatedMillis =
                                System.currentTimeMillis(),

                            settlements =
                                newSettlements
                        )

                    stateStore.update(
                        mergedState
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
