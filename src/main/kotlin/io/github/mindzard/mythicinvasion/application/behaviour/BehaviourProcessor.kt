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
    private val profileStore: BehaviourProfileStore
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

                    for (event in events) {
                        profileStore.apply(event)
                    }

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
         * Process whatever is still available before shutdown.
         *
         * This operation only touches our own Kotlin data structures,
         * so it does not require the Minecraft main thread.
         */
        val remainingEvents = buffer.drain(
            maxEvents = 100_000
        )

        for (event in remainingEvents) {
            profileStore.apply(event)
        }
    }
}
