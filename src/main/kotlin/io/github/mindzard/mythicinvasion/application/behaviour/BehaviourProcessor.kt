package io.github.mindzard.mythicinvasion.application.behaviour

import io.github.mindzard.mythicinvasion.concurrency.CoroutineEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.bukkit.plugin.java.JavaPlugin

class BehaviourProcessor(
    private val plugin: JavaPlugin,
    private val coroutineEngine: CoroutineEngine,
    private val buffer: BehaviourEventBuffer,
    private val profileStore: BehaviourProfileStore,
    private val featureEngine: BehaviourFeatureEngine
) {

    private var processingJob: Job? = null

    fun start() {
        if (processingJob != null) {
            return
        }

        processingJob = coroutineEngine.scope.launch {

            while (isActive) {

                val events = buffer.drain(
                    maxEvents = 1_000
                )

                if (events.isNotEmpty()) {
                    processBatch(events)

                    plugin.logger.fine(
                        "Processed ${events.size} behaviour events. " +
                            "Buffered=${buffer.size()}"
                    )
                }

                delay(1_000L)
            }
        }
    }

    fun stop() {

        processingJob?.cancel()
        processingJob = null

        /*
         * Drain pending events before shutdown.
         */
        val remainingEvents = buffer.drain(
            maxEvents = 100_000
        )

        if (remainingEvents.isNotEmpty()) {
            processBatch(
                remainingEvents
            )

            plugin.logger.fine(
                "Processed ${remainingEvents.size} pending " +
                    "behaviour events during shutdown."
            )
        }
    }

    private fun processBatch(
        events: List<io.github.mindzard.mythicinvasion.domain.behaviour.BehaviourEvent>
    ) {

        for (event in events) {
            profileStore.apply(event)
        }

        /*
         * Only recalculate profiles that were actually affected by
         * the current batch.
         *
         * Distinct UUIDs prevent unnecessary duplicate calculations.
         */
        events
            .asSequence()
            .map { it.playerId }
            .distinct()
            .forEach { playerId ->

                profileStore.recalculateFeatures(
                    playerId = playerId,
                    featureEngine = featureEngine
                )
            }
    }
}
