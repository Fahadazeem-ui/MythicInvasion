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
        return configuration.getBoolean("plugin.debug", false)
    }

    fun isEcosystemEnabled(): Boolean {
        return configuration.getBoolean("ecosystem.enabled", true)
    }

    fun ecosystemUpdateIntervalTicks(): Long {
        return configuration
            .getLong("ecosystem.update-interval-ticks", 20L)
            .coerceAtLeast(1L)
    }

    fun isAiEnabled(): Boolean {
        return configuration.getBoolean("ai.enabled", false)
    }

    fun isDatabaseEnabled(): Boolean {
        return configuration.getBoolean("database.enabled", false)
    }
}
