package io.github.mindzard.mythicinvasion.bootstrap

import io.github.mindzard.mythicinvasion.MythicInvasionPlugin
import io.github.mindzard.mythicinvasion.application.behaviour.BehaviourDecayEngine
import io.github.mindzard.mythicinvasion.application.behaviour.BehaviourEventBuffer
import io.github.mindzard.mythicinvasion.application.behaviour.BehaviourFeatureEngine
import io.github.mindzard.mythicinvasion.application.behaviour.BehaviourProcessor
import io.github.mindzard.mythicinvasion.application.behaviour.BehaviourProfileStore
import io.github.mindzard.mythicinvasion.application.ecosystem.EcosystemCoordinator
import io.github.mindzard.mythicinvasion.application.ecosystem.EcosystemEngine
import io.github.mindzard.mythicinvasion.application.intelligence.BehaviourIntelligenceEngine
import io.github.mindzard.mythicinvasion.application.intelligence.BehaviourIntelligenceStore
import io.github.mindzard.mythicinvasion.application.world.WorldIntelligenceCoordinator
import io.github.mindzard.mythicinvasion.application.world.WorldIntelligenceEngine
import io.github.mindzard.mythicinvasion.application.world.WorldStateStore
import io.github.mindzard.mythicinvasion.concurrency.CoroutineEngine
import io.github.mindzard.mythicinvasion.config.ConfigurationManager
import io.github.mindzard.mythicinvasion.infrastructure.paper.ecosystem.PlayerSnapshotCollector
import io.github.mindzard.mythicinvasion.infrastructure.paper.world.WorldSnapshotCollector
import io.github.mindzard.mythicinvasion.infrastructure.paper.player.PlayerBehaviourListener

class PluginBootstrap(
    private val plugin: MythicInvasionPlugin
) {

    fun start(): ServiceRegistry {

        val registry =
            ServiceRegistry()

        val configurationManager =
            ConfigurationManager(
                plugin
            )

        configurationManager.load()

        registry.registerConfigurationManager(
            configurationManager
        )

        val coroutineEngine =
            CoroutineEngine(
                plugin
            )

        registry.registerCoroutineEngine(
            coroutineEngine
        )

        val playerSnapshotCollector =
            PlayerSnapshotCollector(
                plugin
            )

        registry.registerPlayerSnapshotCollector(
            playerSnapshotCollector
        )

        val ecosystemEngine =
            EcosystemEngine(
                plugin
            )

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

        val decayEngine =
            BehaviourDecayEngine(
                halfLifeMillis =
                    configurationManager
                        .behaviourDecayHalfLifeMillis()
            )

        val behaviourProfileStore =
            BehaviourProfileStore(
                decayEngine
            )

        registry.registerBehaviourProfileStore(
            behaviourProfileStore
        )

        val behaviourFeatureEngine =
            BehaviourFeatureEngine()

        registry.registerBehaviourFeatureEngine(
            behaviourFeatureEngine
        )

        val behaviourIntelligenceEngine =
            BehaviourIntelligenceEngine()

        registry.registerBehaviourIntelligenceEngine(
            behaviourIntelligenceEngine
        )

        val behaviourIntelligenceStore =
            BehaviourIntelligenceStore(
                behaviourIntelligenceEngine
            )

        registry.registerBehaviourIntelligenceStore(
            behaviourIntelligenceStore
        )

        val behaviourProcessor =
            BehaviourProcessor(
                plugin = plugin,
                coroutineEngine = coroutineEngine,
                buffer = behaviourEventBuffer,
                profileStore = behaviourProfileStore,
                featureEngine = behaviourFeatureEngine,
                intelligenceStore = behaviourIntelligenceStore,
                processingIntervalMillis = {
                    configurationManager
                        .behaviourProcessingIntervalMillis()
                }
            )

        registry.registerBehaviourProcessor(
            behaviourProcessor
        )

        /*
         * World intelligence subsystem.
         */

        val worldSnapshotCollector =
            WorldSnapshotCollector(
                plugin
            )

        registry.registerWorldSnapshotCollector(
            worldSnapshotCollector
        )

        val worldIntelligenceEngine =
            WorldIntelligenceEngine()

        registry.registerWorldIntelligenceEngine(
            worldIntelligenceEngine
        )

        val worldStateStore =
            WorldStateStore()

        registry.registerWorldStateStore(
            worldStateStore
        )

        val worldIntelligenceCoordinator =
            WorldIntelligenceCoordinator(
                plugin = plugin,
                coroutineEngine = coroutineEngine,
                collector = worldSnapshotCollector,
                engine = worldIntelligenceEngine,
                stateStore = worldStateStore,
                updateIntervalMillis = {
                    configurationManager
                        .worldIntelligenceUpdateIntervalMillis()
                }
            )

        registry.registerWorldIntelligenceCoordinator(
            worldIntelligenceCoordinator
        )

        /*
         * Minecraft listeners.
         */
        plugin.server.pluginManager.registerEvents(
            PlayerBehaviourListener(
                buffer = behaviourEventBuffer
            ),
            plugin
        )

        /*
         * Start enabled systems.
         */
        if (
            configurationManager
                .isBehaviourEnabled()
        ) {
            behaviourProcessor.start()
        }

        if (
            configurationManager
                .isEcosystemEnabled()
        ) {
            ecosystemCoordinator.start()
        }

        if (
            configurationManager
                .isWorldIntelligenceEnabled()
        ) {
            worldIntelligenceCoordinator.start()
        }

        plugin.logger.info(
            "Configuration system initialized."
        )

        plugin.logger.info(
            "Coroutine engine initialized."
        )

        plugin.logger.info(
            "Behaviour systems initialized."
        )

        plugin.logger.info(
            "Ecosystem engine initialized."
        )

        plugin.logger.info(
            "World snapshot collector initialized."
        )

        plugin.logger.info(
            "World intelligence engine initialized."
        )

        plugin.logger.info(
            "World intelligence coordinator started."
        )

        return registry
    }
}
