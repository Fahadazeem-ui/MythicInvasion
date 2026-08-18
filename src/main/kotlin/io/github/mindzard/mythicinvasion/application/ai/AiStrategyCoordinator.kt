package io.github.mindzard.mythicinvasion.application.ai

import io.github.mindzard.mythicinvasion.concurrency.CoroutineEngine
import io.github.mindzard.mythicinvasion.domain.ai.AiDecision
import io.github.mindzard.mythicinvasion.infrastructure.ai.GeminiStrategyClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.bukkit.plugin.java.JavaPlugin

class AiStrategyCoordinator(
    private val plugin: JavaPlugin,
    private val coroutineEngine: CoroutineEngine,
    private val contextAssembler: AiContextAssembler,
    private val geminiClient: GeminiStrategyClient,
    private val validator: AiDecisionValidator,
    private val updateIntervalMillis: () -> Long,
    private val maxContextCharacters: Int
) {

    private var job: Job? = null

    @Volatile
    private var latestDecision: AiDecision? = null

    fun start() {

        if (
            job != null
        ) {
            return
        }

        if (
            !geminiClient.isConfigured()
        ) {

            plugin.logger.warning(
                "AI strategy layer is enabled, " +
                    "but GOOGLE_API_KEY is not configured."
            )

            return
        }

        job =
            coroutineEngine.scope.launch {

                plugin.logger.info(
                    "Gemini AI strategy coordinator started."
                )

                while (isActive) {

                    try {

                        val context =
                            contextAssembler
                                .assemble()

                        val contextJson =
                            contextAssembler
                                .toJson(
                                    context =
                                        context,
                                    maximumCharacters =
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

                            val validated =
                                validator.validate(
                                    decision
                                )

                            if (
                                validated != null
                            ) {

                                latestDecision =
                                    validated

                                plugin.logger.info(
                                    "AI strategy accepted: " +
                                        validated.strategyId +
                                        " | priority=" +
                                        validated.priority +
                                        " | confidence=" +
                                        "%.2f".format(
                                            validated.confidence
                                        )
                                )

                            } else {

                                plugin.logger.warning(
                                    "AI strategy was rejected " +
                                        "by the local validator."
                                )
                            }
                        }

                    } catch (
                        exception: Exception
                    ) {

                        plugin.logger.warning(
                            "AI strategy cycle failed: " +
                                "${exception.javaClass.simpleName}: " +
                                "${exception.message}"
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

        plugin.logger.info(
            "Gemini AI strategy coordinator stopped."
        )
    }
}
