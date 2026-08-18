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

    fun isDatabaseEnabled(): Boolean {
        return configuration.getBoolean(
            "database.enabled",
            false
        )
    }
}
