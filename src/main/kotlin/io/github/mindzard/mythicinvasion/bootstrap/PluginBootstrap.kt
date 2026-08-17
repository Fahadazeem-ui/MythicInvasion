package io.github.mindzard.mythicinvasion.bootstrap

import io.github.mindzard.mythicinvasion.MythicInvasionPlugin
import io.github.mindzard.mythicinvasion.application.ecosystem.EcosystemCoordinator
import io.github.mindzard.mythicinvasion.application.ecosystem.EcosystemEngine
import io.github.mindzard.mythicinvasion.concurrency.CoroutineEngine
import io.github.mindzard.mythicinvasion.config.ConfigurationManager
import io.github.mindzard.mythicinvasion.infrastructure.paper.ecosystem.PlayerSnapshotCollector
import io.github.mindzard.mythicinvasion.infrastructure.paper.player.PlayerBehaviourListener

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

        val playerSnapshotCollector =
            PlayerSnapshotCollector(plugin)

        registry.registerPlayerSnapshotCollector(
            playerSnapshotCollector
        )

        val ecosystemEngine =
            EcosystemEngine(plugin)

        registry.registerEcosystemEngine(
            ecosystemEngine
        )

        val ecosystemCoordinator =
            EcosystemCoordinator(
                plugin = plugin,
                coroutineEngine = coroutineEngine,
                snapshotCollector = playerSnapshotCollector,
                ecosystemEngine = ecosystemEngine,
                updateIntervalMillisProvider = {
                    configurationManager
                        .ecosystemUpdateIntervalTicks()
                        .times(50L)
                }
            )

        registry.registerEcosystemCoordinator(
            ecosystemCoordinator
        )

        /*
         * Register the player behaviour listener with Bukkit/Paper.
         *
         * From this point onward Minecraft will send relevant
         * player events to PlayerBehaviourListener.
         */
        plugin.server.pluginManager.registerEvents(
            PlayerBehaviourListener(),
            plugin
        )

        ecosystemCoordinator.start()

        plugin.logger.info(
            "Configuration system initialized."
        )

        plugin.logger.info(
            "Coroutine engine initialized."
        )

        plugin.logger.info(
            "Player snapshot collector initialized."
        )

        plugin.logger.info(
            "Player behaviour listener registered."
        )

        plugin.logger.info(
            "Ecosystem engine initialized."
        )

        return registry
    }
}
