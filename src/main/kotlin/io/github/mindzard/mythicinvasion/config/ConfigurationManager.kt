package io.github.mindzard.mythicinvasion.config

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin

class ConfigurationManager(
    private val plugin: JavaPlugin
) {

    private lateinit var configuration: FileConfiguration

    fun load() {
        plugin.saveDefaultConfig()
        plugin.reloadConfig()

        configuration = plugin.config
    }

    fun isDebugEnabled(): Boolean {
        return configuration.getBoolean(
            "plugin.debug",
            false
        )
    }

    fun isEcosystemEnabled(): Boolean {
        return configuration.getBoolean(
            "ecosystem.enabled",
            true
        )
    }

    fun ecosystemUpdateIntervalTicks(): Long {
        return configuration
            .getLong(
                "ecosystem.update-interval-ticks",
                20L
            )
            .coerceAtLeast(1L)
    }

    fun isWorldIntelligenceEnabled(): Boolean {
        return configuration.getBoolean(
            "world-intelligence.enabled",
            true
        )
    }

    fun worldIntelligenceUpdateIntervalMillis(): Long {
        return configuration
            .getLong(
                "world-intelligence.update-interval-millis",
                5_000L
            )
            .coerceAtLeast(1_000L)
    }

    fun isSocietyEnabled(): Boolean {
        return configuration.getBoolean(
            "society.enabled",
            true
        )
    }

    fun societyObservationIntervalMillis(): Long {
        return configuration
            .getLong(
                "society.observation-interval-millis",
                10_000L
            )
            .coerceAtLeast(2_000L)
    }

    fun villagerAnalysisIntervalMillis(): Long {
        return configuration
            .getLong(
                "society.villager-analysis-interval-millis",
                10_000L
            )
            .coerceAtLeast(2_000L)
    }

    fun societySocialIntervalMillis(): Long {
        return configuration
            .getLong(
                "society.social-interval-millis",
                10_000L
            )
            .coerceAtLeast(2_000L)
    }

    fun isBehaviourEnabled(): Boolean {
        return configuration.getBoolean(
            "behaviour.enabled",
            true
        )
    }

    fun behaviourProcessingIntervalMillis(): Long {
        return configuration
            .getLong(
                "behaviour.processing-interval-millis",
                1_000L
            )
            .coerceAtLeast(50L)
    }

    fun behaviourDecayHalfLifeMinutes(): Double {
        return configuration
            .getDouble(
                "behaviour.decay.half-life-minutes",
                30.0
            )
            .coerceAtLeast(0.1)
    }

    fun behaviourDecayHalfLifeMillis(): Long {
        return (
            behaviourDecayHalfLifeMinutes() *
                60_000.0
            )
            .toLong()
            .coerceAtLeast(1L)
    }

    fun isAiEnabled(): Boolean {
        return configuration.getBoolean(
            "ai.enabled",
            false
        )
    }

    fun aiProvider(): String {
        return configuration
            .getString(
                "ai.provider",
                "gemini"
            )
            ?.trim()
            ?.lowercase()
            ?: "gemini"
    }

    fun aiModel(): String {
        return configuration
            .getString(
                "ai.model",
                "gemini-3.6-flash"
            )
            ?.trim()
            ?.ifBlank {
                "gemini-3.6-flash"
            }
            ?: "gemini-3.6-flash"
    }

    fun aiApiKey(): String? {
        return configuration
            .getString(
                "ai.api-key",
                ""
            )
            ?.trim()
            ?.takeIf {
                it.isNotEmpty()
            }
    }

    fun aiMinimumConfidence(): Double {
        return configuration
            .getDouble(
                "ai.minimum-confidence",
                0.65
            )
            .coerceIn(
                0.0,
                1.0
            )
    }

    fun aiStrategyIntervalMillis(): Long {
        return configuration
            .getLong(
                "ai.strategy-interval-millis",
                60_000L
            )
            .coerceAtLeast(10_000L)
    }

    fun aiRequestTimeoutMillis(): Long {
        return configuration
            .getLong(
                "ai.request-timeout-millis",
                30_000L
            )
            .coerceAtLeast(1_000L)
    }

    fun aiMaxContextCharacters(): Int {
        return configuration
            .getInt(
                "ai.max-context-characters",
                12_000
            )
            .coerceAtLeast(1_000)
    }

    fun isAdaptiveBehaviourEnabled(): Boolean {
        return configuration.getBoolean(
            "adaptive-behaviour.enabled",
            true
        )
    }

    fun isAdaptiveHostileTargetingEnabled(): Boolean {
        return configuration.getBoolean(
            "adaptive-behaviour.hostile-targeting.enabled",
            true
        )
    }

    fun adaptiveTargetRange(): Double {
        return configuration
            .getDouble(
                "adaptive-behaviour.hostile-targeting.range",
                32.0
            )
            .coerceIn(
                8.0,
                64.0
            )
    }

    fun adaptiveTargetMinimumAdvantage(): Double {
        return configuration
            .getDouble(
                "adaptive-behaviour.hostile-targeting.minimum-advantage",
                0.12
            )
            .coerceIn(
                0.01,
                0.50
            )
    }

    fun adaptiveTargetCooldownMillis(): Long {
        return configuration
            .getLong(
                "adaptive-behaviour.hostile-targeting.cooldown-millis",
                5_000L
            )
            .coerceIn(
                1_000L,
                30_000L
            )
    }

    fun isVillagerCitizenBehaviourEnabled(): Boolean {
        return configuration.getBoolean(
            "society.villager-citizen-behaviour.enabled",
            true
        )
    }

    fun villagerCitizenUpdateIntervalMillis(): Long {
        return configuration
            .getLong(
                "society.villager-citizen-behaviour.update-interval-millis",
                5_000L
            )
            .coerceIn(
                1_000L,
                30_000L
            )
    }

    fun villagerCitizenInteractionRadius(): Double {
        return configuration
            .getDouble(
                "society.villager-citizen-behaviour.interaction-radius",
                12.0
            )
            .coerceIn(
                4.0,
                32.0
            )
    }

    fun villagerCitizenHostileRadius(): Double {
        return configuration
            .getDouble(
                "society.villager-citizen-behaviour.hostile-radius",
                10.0
            )
            .coerceIn(
                4.0,
                24.0
            )
    }

    fun villagerCitizenFriendlyLookRadius(): Double {
        return configuration
            .getDouble(
                "society.villager-citizen-behaviour.friendly-look-radius",
                10.0
            )
            .coerceIn(
                4.0,
                24.0
            )
    }

    fun villagerCitizenHostileThreatThreshold(): Double {
        return configuration
            .getDouble(
                "society.villager-citizen-behaviour.hostile-threat-threshold",
                0.65
            )
            .coerceIn(
                0.1,
                1.0
            )
    }

    fun villagerCitizenFriendlyTrustThreshold(): Double {
        return configuration
            .getDouble(
                "society.villager-citizen-behaviour.friendly-trust-threshold",
                0.60
            )
            .coerceIn(
                0.1,
                1.0
            )
    }

    fun villagerCitizenActionCooldownMillis(): Long {
        return configuration
            .getLong(
                "society.villager-citizen-behaviour.action-cooldown-millis",
                5_000L
            )
            .coerceIn(
                1_000L,
                30_000L
            )
    }

    fun isPillagerFactionIntelligenceEnabled(): Boolean {
        return configuration.getBoolean(
            "society.pillager-faction-intelligence.enabled",
            true
        )
    }

    fun pillagerFactionUpdateIntervalMillis(): Long {
        return configuration
            .getLong(
                "society.pillager-faction-intelligence.update-interval-millis",
                10_000L
            )
            .coerceIn(
                2_000L,
                60_000L
            )
    }

    fun isDatabaseEnabled(): Boolean {
        return configuration.getBoolean(
            "database.enabled",
            false
        )
    }
}
