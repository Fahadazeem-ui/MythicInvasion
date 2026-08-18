package io.github.mindzard.mythicinvasion.bootstrap

import io.github.mindzard.mythicinvasion.MythicInvasionPlugin
import io.github.mindzard.mythicinvasion.application.behaviour.BehaviourEventBuffer
import io.github.mindzard.mythicinvasion.application.behaviour.BehaviourFeatureEngine
import io.github.mindzard.mythicinvasion.application.behaviour.BehaviourProcessor
import io.github.mindzard.mythicinvasion.application.behaviour.BehaviourProfileStore
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
         * Behaviour subsystem.
         */

        val behaviourEventBuffer =
            BehaviourEventBuffer()

        registry.registerBehaviourEventBuffer(
            behaviourEventBuffer
        )

        val behaviourProfileStore =
            BehaviourProfileStore()

        registry.registerBehaviourProfileStore(
            behaviourProfileStore
        )

        val behaviourFeatureEngine =
            BehaviourFeatureEngine()

        registry.registerBehaviourFeatureEngine(
            behaviourFeatureEngine
        )

        val behaviourProcessor =
            BehaviourProcessor(
                plugin = plugin,
                coroutineEngine = coroutineEngine,
                buffer = behaviourEventBuffer,
                profileStore = behaviourProfileStore,
                featureEngine = behaviourFeatureEngine
            )

        registry.registerBehaviourProcessor(
            behaviourProcessor
        )

        /*
         * Register Bukkit/Paper event listeners.
         */
        plugin.server.pluginManager.registerEvents(
            PlayerBehaviourListener(
                buffer = behaviourEventBuffer
            ),
            plugin
        )

        /*
         * Start background processing.
         */
        behaviourProcessor.start()
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
            "Ecosystem engine initialized."
        )

        plugin.logger.info(
            "Behaviour event buffer initialized."
        )

        plugin.logger.info(
            "Behaviour profile store initialized."
        )

        plugin.logger.info(
            "Behaviour feature engine initialized."
        )

        plugin.logger.info(
            "Behaviour processor started."
        )

        plugin.logger.info(
            "Player behaviour listener registered."
        )

        return registry
    }
}
