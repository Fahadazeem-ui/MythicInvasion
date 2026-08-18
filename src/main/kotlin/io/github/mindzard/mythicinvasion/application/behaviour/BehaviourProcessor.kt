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
    private val featureEngine: BehaviourFeatureEngine,
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
                     * Apply time decay even when a player has generated
                     * no new events.
                     */
                    profileStore.applyTimeDecay(
                        nowMillis
                    )

                    val events =
                        buffer.drain(
                            maxEvents = 1_000
                        )

                    if (events.isNotEmpty()) {

                        processBatch(
                            events
                        )
                    }

                    /*
                     * Recalculate every existing profile after decay.
                     *
                     * This ensures AI-facing features also reflect the
                     * latest decayed state.
                     */
                    profileStore.recalculateAllFeatures(
                        featureEngine
                    )

                    if (
                        plugin.logger.isLoggable(
                            java.util.logging.Level.FINE
                        )
                    ) {

                        plugin.logger.fine(
                            "Behaviour cycle completed. " +
                                "processedEvents=" +
                                events.size +
                                ", profiles=" +
                                profileStore.snapshot().size +
                                ", buffered=" +
                                buffer.size()
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
         * Process any remaining events before shutdown.
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
    }

    private fun processBatch(
        events: List<
            io.github.mindzard.mythicinvasion.domain.behaviour.BehaviourEvent
            >
    ) {

        for (event in events) {

            profileStore.apply(
                event
            )
        }
    }
}
