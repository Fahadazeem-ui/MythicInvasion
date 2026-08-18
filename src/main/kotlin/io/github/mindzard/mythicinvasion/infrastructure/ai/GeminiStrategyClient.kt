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
    private val model: String,
    private val timeoutMillis: Long
) {

    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    private val client: Client? =
        System.getenv("GOOGLE_API_KEY")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { apiKey ->
                Client
                    .builder()
                    .apiKey(apiKey)
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

            /*
             * The Google Gen AI Java SDK exposes async model generation
             * through client.async.models.generateContent(...).
             */
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
            - never directly control individual entities;
            - prefer strategies executable by a deterministic
              local game engine;
            - preserve player agency;
            - avoid repetitive escalation;
            - consider settlement safety;
            - consider player relationships;
            - consider current world state.

            Return ONLY valid JSON with this exact structure:

            {
              "strategyId": "string",
              "priority": 0,
              "summary": "string",
              "reasoning": "string",
              "confidence": 0.0,
              "suggestedActions": [
                "string"
              ]
            }

            World context:

            $contextJson
        """.trimIndent()
    }

    private fun parseDecision(
        rawText: String?
    ): AiDecision? {

        if (rawText.isNullOrBlank()) {
            return null
        }

        return try {

            /*
             * Gemini is instructed to return JSON, but models can
             * occasionally wrap JSON in markdown fences.
             *
             * Clean those fences before parsing.
             */
            val cleaned =
                cleanJsonResponse(
                    rawText
                )

            val rootElement =
                json.parseToJsonElement(
                    cleaned
                )

            val rootObject: JsonObject =
                rootElement.jsonObject

            val strategyId =
                rootObject
                    .stringValue(
                        "strategyId"
                    )
                    ?: return null

            val priority =
                rootObject
                    .intValue(
                        "priority"
                    )
                    ?: return null

            val summary =
                rootObject
                    .stringValue(
                        "summary"
                    )
                    ?: return null

            val reasoning =
                rootObject
                    .stringValue(
                        "reasoning"
                    )
                    ?: return null

            val confidence =
                rootObject
                    .doubleValue(
                        "confidence"
                    )
                    ?: return null

            val suggestedActions =
                rootObject
                    .arrayOfStrings(
                        "suggestedActions"
                    )
                    .take(
                        10
                    )
                    .map {
                        it.take(250)
                    }

            return AiDecision(
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

        } catch (exception: Exception) {

            plugin.logger.warning(
                "Gemini returned an invalid strategic JSON response."
            )

            plugin.logger.fine(
                "Gemini raw response: $rawText"
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

        val primitive: JsonPrimitive =
            element.jsonPrimitive

        return primitive
            .content
            .trim()
            .ifBlank {
                null
            }
    }

    private fun JsonObject.intValue(
        key: String
    ): Int? {

        val element =
            this[key]
                ?: return null

        return element
            .jsonPrimitive
            .intOrNull
    }

    private fun JsonObject.doubleValue(
        key: String
    ): Double? {

        val element =
            this[key]
                ?: return null

        return element
            .jsonPrimitive
            .doubleOrNull
    }

    private fun JsonObject.arrayOfStrings(
        key: String
    ): List<String> {

        val element =
            this[key]
                ?: return emptyList()

        val array: JsonArray =
            element.jsonArray

        return array
            .mapNotNull { element ->

                val primitive =
                    element
                        .jsonPrimitive

                primitive
                    .content
                    .trim()
                    .ifBlank {
                        null
                    }
            }
    }
}
