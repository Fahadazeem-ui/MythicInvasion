package io.github.mindzard.mythicinvasion.infrastructure.ai

import com.google.genai.Client
import com.google.genai.types.GenerateContentConfig
import io.github.mindzard.mythicinvasion.domain.ai.AiDecision
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.TimeUnit

class GeminiStrategyClient(
    private val plugin: JavaPlugin,
    private val apiKey: String?,
    private val model: String,
    private val timeoutMillis: Long
) {

    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    private val client: Client? =
        apiKey
            ?.trim()
            ?.takeIf {
                it.isNotEmpty()
            }
            ?.let { key ->
                Client
                    .builder()
                    .apiKey(key)
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

            Return exactly one high-level strategic decision.

            The decision must:
            - be believable;
            - use only supplied world information;
            - avoid impossible actions;
            - preserve player agency;
            - avoid repetitive escalation;
            - consider settlement safety;
            - consider social relationships;
            - be executable later by a deterministic local engine.

            Return ONLY valid JSON:

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

            val cleaned =
                cleanJsonResponse(
                    rawText
                )

            val root =
                json
                    .parseToJsonElement(
                        cleaned
                    )
                    .jsonObject

            val strategyId =
                root.stringValue(
                    "strategyId"
                )
                    ?: return null

            val priority =
                root.intValue(
                    "priority"
                )
                    ?: return null

            val summary =
                root.stringValue(
                    "summary"
                )
                    ?: return null

            val reasoning =
                root.stringValue(
                    "reasoning"
                )
                    ?: return null

            val confidence =
                root.doubleValue(
                    "confidence"
                )
                    ?: return null

            val suggestedActions =
                root.arrayOfStrings(
                    "suggestedActions"
                )
                    .take(10)
                    .map {
                        it.take(250)
                    }

            AiDecision(
                strategyId =
                    strategyId
                        .trim()
                        .take(100),

                priority =
                    priority
                        .coerceIn(
                            0,
                            100
                        ),

                summary =
                    summary
                        .trim()
                        .take(500),

                reasoning =
                    reasoning
                        .trim()
                        .take(2_000),

                confidence =
                    confidence
                        .coerceIn(
                            0.0,
                            1.0
                        ),

                suggestedActions =
                    suggestedActions,

                generatedAtMillis =
                    System.currentTimeMillis()
            )

        } catch (
            exception: Exception
        ) {

            plugin.logger.warning(
                "Gemini returned invalid JSON."
            )

            null
        }
    }

    private fun cleanJsonResponse(
        rawText: String
    ): String {

        var cleaned =
            rawText.trim()

        if (
            cleaned.startsWith(
                "```"
            )
        ) {

            cleaned =
                cleaned
                    .removePrefix(
                        "```json"
                    )
                    .removePrefix(
                        "```JSON"
                    )
                    .removePrefix(
                        "```"
                    )
                    .trim()

            if (
                cleaned.endsWith(
                    "```"
                )
            ) {

                cleaned =
                    cleaned
                        .removeSuffix(
                            "```"
                        )
                        .trim()
            }
        }

        return cleaned
    }

    private fun JsonObject.stringValue(
        key: String
    ): String? {

        val element =
            this[key]
                ?: return null

        val primitive:
            JsonPrimitive =
            element.jsonPrimitive

        return primitive.content
            .trim()
            .ifBlank {
                null
            }
    }

    private fun JsonObject.intValue(
        key: String
    ): Int? {

        return this[key]
            ?.jsonPrimitive
            ?.intOrNull
    }

    private fun JsonObject.doubleValue(
        key: String
    ): Double? {

        return this[key]
            ?.jsonPrimitive
            ?.doubleOrNull
    }

    private fun JsonObject.arrayOfStrings(
        key: String
    ): List<String> {

        val element =
            this[key]
                ?: return emptyList()

        val array:
            JsonArray =
            element.jsonArray

        return array.mapNotNull { item ->

            val primitive =
                item.jsonPrimitive

            primitive.content
                .trim()
                .ifBlank {
                    null
                }
        }
    }
}
