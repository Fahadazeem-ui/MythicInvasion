package io.github.mindzard.mythicinvasion.application.society

import io.github.mindzard.mythicinvasion.concurrency.CoroutineEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.bukkit.plugin.java.JavaPlugin

class SettlementSocialCoordinator(
    private val plugin: JavaPlugin,
    private val coroutineEngine: CoroutineEngine,
    private val relationshipStore: VillagerRelationshipStore,
    private val engine: SettlementSocialEngine,
    private val socialStore: SettlementSocialStore,
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

                    val relationships =
                        relationshipStore.snapshot()

                    val profiles =
                        engine.buildProfiles(
                            relationships
                        )

                    socialStore.replaceAll(
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
