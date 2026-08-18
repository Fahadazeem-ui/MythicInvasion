package io.github.mindzard.mythicinvasion.application.ai

import io.github.mindzard.mythicinvasion.concurrency.CoroutineEngine
import io.github.mindzard.mythicinvasion.domain.ai.AiDecision
import io.github.mindzard.mythicinvasion.infrastructure.ai.GeminiStrategyClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bukkit.plugin.java.JavaPlugin

class AiStrategyCoordinator(
    private val plugin: JavaPlugin,
    private val coroutineEngine: CoroutineEngine,
    private val contextAssembler: AiContextAssembler,
    private val geminiClient: GeminiStrategyClient,
    private val updateIntervalMillis: () -> Long,
    private val maxContextCharacters: Int
) {

    private val json =
        Json {
            encodeDefaults = true
        }

    private var job: Job? = null

    @Volatile
    private var latestDecision: AiDecision? = null

    fun start() {

        if (job != null) {
            return
        }

        if (
            !geminiClient.isConfigured()
        ) {

            plugin.logger.info(
                "AI strategy layer is disabled because " +
                    "GOOGLE_API_KEY is not configured."
            )

            return
        }

        job =
            coroutineEngine.scope.launch {

                while (isActive) {

                    try {

                        val context =
                            contextAssembler
                                .assemble()

                        val contextJson =
                            json
                                .encodeToString(
                                    context
                                )
                                .take(
                                    maxContextCharacters
                                )

                        val decision =
                            geminiClient
                                .generateStrategy(
                                    contextJson
                                )

                        if (
                            decision != null
                        ) {

                            latestDecision =
                                decision

                            plugin.logger.info(
                                "AI strategy updated: " +
                                    decision.strategyId +
                                    " (confidence=" +
                                    "%.2f".format(
                                        decision.confidence
                                    ) +
                                    ")"
                            )
                        }

                    } catch (
                        exception: Exception
                    ) {

                        plugin.logger.warning(
                            "AI strategy cycle failed: " +
                                exception.message
                        )
                    }

                    delay(
                        updateIntervalMillis()
                    )
                }
            }
    }

    fun currentDecision(): AiDecision? {
        return latestDecision
    }

    fun stop() {
        job?.cancel()
        job = null
        latestDecision = null
    }
}
