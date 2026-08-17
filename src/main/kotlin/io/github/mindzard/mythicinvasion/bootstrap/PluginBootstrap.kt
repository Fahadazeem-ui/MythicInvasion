package io.github.mindzard.mythicinvasion.bootstrap

import io.github.mindzard.mythicinvasion.MythicInvasionPlugin
import io.github.mindzard.mythicinvasion.concurrency.CoroutineEngine
import io.github.mindzard.mythicinvasion.config.ConfigurationManager

class PluginBootstrap(
    private val plugin: MythicInvasionPlugin
) {

    fun start(): ServiceRegistry {
        val registry = ServiceRegistry()

        val configurationManager =
            ConfigurationManager(plugin)

        configurationManager.load()

        registry.registerConfigurationManager(
            configurationManager
        )

        val coroutineEngine =
            CoroutineEngine(plugin)

        registry.registerCoroutineEngine(
            coroutineEngine
        )

        plugin.logger.info(
            "Configuration system initialized."
        )

        plugin.logger.info(
            "Coroutine engine initialized."
        )

        return registry
    }
}
