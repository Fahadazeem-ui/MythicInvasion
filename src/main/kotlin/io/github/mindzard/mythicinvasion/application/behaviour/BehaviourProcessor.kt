package io.github.mindzard.mythicinvasion.application.behaviour

import io.github.mindzard.mythicinvasion.application.intelligence.BehaviourIntelligenceStore
import io.github.mindzard.mythicinvasion.concurrency.CoroutineEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level

class BehaviourProcessor(
    private val plugin: JavaPlugin,
    private val coroutineEngine: CoroutineEngine,
    private val buffer: BehaviourEventBuffer,
    private val profileStore: BehaviourProfileStore,
    private val featureEngine: BehaviourFeatureEngine,
    private val intelligenceStore: BehaviourIntelligenceStore,
    private val processingIntervalMillis: () -> Long
) {

    private var processingJob: Job? = null

    fun start() {

        if (processingJob != null) {
            return
        }

        processingJob =
            coroutineEngine.scope.launch {

                while (isActive) {

                    val nowMillis =
                        System.currentTimeMillis()

                    /*
                     * Update the weighted behaviour state even when
                     * the player produces no new events.
                     */
                    profileStore.applyTimeDecay(
                        nowMillis
                    )

                    val events =
                        buffer.drain(
                            maxEvents = 1_000
                        )

                    if (events.isNotEmpty()) {
                        processBatch(events)
                    }

                    /*
                     * Recalculate current behaviour features.
                     */
                    profileStore.recalculateAllFeatures(
                        featureEngine
                    )

                    /*
                     * Build/update AI-facing intelligence profiles.
                     */
                    updateIntelligenceProfiles()

                    if (
                        plugin.logger.isLoggable(
                            Level.FINE
                        )
                    ) {

                        plugin.logger.fine(
                            "Behaviour cycle completed. " +
                                "processedEvents=${events.size}, " +
                                "behaviourProfiles=${profileStore.snapshot().size}, " +
                                "intelligenceProfiles=${intelligenceStore.snapshot().size}, " +
                                "buffered=${buffer.size()}"
                        )
                    }

                    delay(
                        processingIntervalMillis()
                    )
                }
            }
    }

    fun stop() {

        processingJob?.cancel()
        processingJob = null

        /*
         * Process any events still waiting in memory.
         */
        val remainingEvents =
            buffer.drain(
                maxEvents = 100_000
            )

        if (remainingEvents.isNotEmpty()) {
            processBatch(
                remainingEvents
            )
        }

        profileStore.applyTimeDecay(
            System.currentTimeMillis()
        )

        profileStore.recalculateAllFeatures(
            featureEngine
        )

        updateIntelligenceProfiles()
    }

    private fun processBatch(
        events: List<
            io.github.mindzard.mythicinvasion.domain.behaviour.BehaviourEvent
            >
    ) {

        for (event in events) {
            profileStore.apply(event)
        }
    }

    private fun updateIntelligenceProfiles() {

        val behaviourProfiles =
            profileStore.snapshot()

        for (profile in behaviourProfiles) {
            intelligenceStore.update(
                behaviourProfile = profile
            )
        }
    }
}
