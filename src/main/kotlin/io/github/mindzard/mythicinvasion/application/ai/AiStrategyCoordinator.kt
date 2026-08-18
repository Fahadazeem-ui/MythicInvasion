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
    private val executionState: StrategyExecutionState,
    private val updateIntervalMillis: () -> Long,
    private val maxContextCharacters: Int
) {

    private var job: Job? = null

    fun start() {

        if (job != null) {
            return
        }

        if (!geminiClient.isConfigured()) {

            plugin.logger.warning(
                "Gemini AI is enabled but no API key is configured."
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
                                    context = context,
                                    maximumCharacters =
                                        maxContextCharacters
                                )

                        val decision =
                            geminiClient
                                .generateStrategy(
                                    contextJson
                                )

                        if (decision != null) {

                            val validated =
                                validator.validate(
                                    decision
                                )

                            if (validated != null) {

                                executionState.update(
                                    validated
                                )

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
        return executionState.current()
    }

    fun stop() {

        job?.cancel()
        job = null

        executionState.clear()

        plugin.logger.info(
            "Gemini AI strategy coordinator stopped."
        )
    }
}
