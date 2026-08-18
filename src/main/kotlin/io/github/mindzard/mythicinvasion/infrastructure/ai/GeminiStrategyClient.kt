package io.github.mindzard.mythicinvasion.infrastructure.ai

import com.google.genai.Client
import com.google.genai.types.GenerateContentConfig
import io.github.mindzard.mythicinvasion.domain.ai.AiDecision
import io.github.mindzard.mythicinvasion.domain.ai.AiStrategicContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.double
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.booleanOrNull
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.TimeUnit

class GeminiStrategyClient(
    private val plugin: JavaPlugin,
    private val model: String,
    private val timeoutMillis: Long
) {

    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    private val client: Client? =
        System.getenv("GOOGLE_API_KEY")
            ?.trim()
            ?.takeIf {
                it.isNotEmpty()
            }
            ?.let {
                Client
                    .builder()
                    .apiKey(it)
                    .build()
            }

    fun isConfigured(): Boolean {
        return client != null
    }

    suspend fun generateStrategy(
        contextJson: String
    ): AiDecision? {

        val activeClient =
            client
                ?: return null

        val prompt =
            buildPrompt(
                contextJson
            )

        return try {

            val future =
                activeClient
                    .async
                    .models
                    .generateContent(
                        model,
                        prompt,
                        GenerateContentConfig
                            .builder()
                            .responseMimeType(
                                "application/json"
                            )
                            .build()
                    )

            val response =
                future
                    .orTimeout(
                        timeoutMillis,
                        TimeUnit.MILLISECONDS
                    )
                    .get()

            parseDecision(
                response.text()
            )

        } catch (exception: Exception) {

            plugin.logger.warning(
                "Gemini strategy request failed: " +
                    "${exception.javaClass.simpleName}: " +
                    "${exception.message}"
            )

            null
        }
    }

    private fun buildPrompt(
        contextJson: String
    ): String {

        return """
            You are the strategic intelligence layer of a
            Minecraft living-world simulation.

            You do NOT directly control Minecraft entities.

            You must return exactly one high-level strategic decision.

            Your decision must:
            - be conservative and believable;
            - never invent unavailable world facts;
            - never request impossible Minecraft actions;
            - avoid directly controlling individual entities;
            - prefer strategies that can be executed by a deterministic
              local game engine;
            - preserve player agency;
            - avoid repetitive escalation;
            - consider settlement safety and social relationships.

            Return ONLY JSON with this structure:

            {
              "strategyId": "string",
              "priority": 0,
              "summary": "string",
              "reasoning": "string",
              "confidence": 0.0,
              "suggestedActions": ["string"]
            }

            World context:

            $contextJson
        """.trimIndent()
    }

    private fun parseDecision(
        rawText: String?
    ): AiDecision? {

        if (
            rawText.isNullOrBlank()
        ) {
            return null
        }

        return try {

            val objectNode =
                json.parseToJsonElement(
                    rawText
                ).jsonObject

            val strategyId =
                objectNode[
                    "strategyId"
                ]?.jsonPrimitive?.contentOrNull
                    ?: return null

            val priority =
                objectNode[
                    "priority"
                ]?.jsonPrimitive?.int
                    ?: return null

            val summary =
                objectNode[
                    "summary"
                ]?.jsonPrimitive?.contentOrNull
                    ?: return null

            val reasoning =
                objectNode[
                    "reasoning"
                ]?.jsonPrimitive?.contentOrNull
                    ?: return null

            val confidence =
                objectNode[
                    "confidence"
                ]?.jsonPrimitive?.double
                    ?: return null

            val actions =
                objectNode[
                    "suggestedActions"
                ]?.let {
                    element ->
                    element
                        .jsonArray
                        .mapNotNull {
                            it.jsonPrimitive
                                .contentOrNull
                        }
                }
                    ?: emptyList()

            AiDecision(
                strategyId =
                    strategyId,

                priority =
                    priority
                        .coerceIn(
                            0,
                            100
                        ),

                summary =
                    summary.take(500),

                reasoning =
                    reasoning.take(2_000),

                confidence =
                    confidence.coerceIn(
                        0.0,
                        1.0
                    ),

                suggestedActions =
                    actions
                        .take(10)
                        .map {
                            it.take(250)
                        },

                generatedAtMillis =
                    System.currentTimeMillis()
            )

        } catch (exception: Exception) {

            plugin.logger.warning(
                "Gemini returned an invalid strategic response."
            )

            null
        }
    }
}
