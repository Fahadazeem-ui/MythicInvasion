package io.github.mindzard.mythicinvasion.bootstrap

import io.github.mindzard.mythicinvasion.MythicInvasionPlugin
import io.github.mindzard.mythicinvasion.application.ai.AiContextAssembler
import io.github.mindzard.mythicinvasion.application.ai.AiStrategyCoordinator
import io.github.mindzard.mythicinvasion.application.behaviour.BehaviourDecayEngine
import io.github.mindzard.mythicinvasion.application.behaviour.BehaviourEventBuffer
import io.github.mindzard.mythicinvasion.application.behaviour.BehaviourFeatureEngine
import io.github.mindzard.mythicinvasion.application.behaviour.BehaviourProcessor
import io.github.mindzard.mythicinvasion.application.behaviour.BehaviourProfileStore
import io.github.mindzard.mythicinvasion.application.ecosystem.EcosystemCoordinator
import io.github.mindzard.mythicinvasion.application.ecosystem.EcosystemEngine
import io.github.mindzard.mythicinvasion.application.intelligence.BehaviourIntelligenceEngine
import io.github.mindzard.mythicinvasion.application.intelligence.BehaviourIntelligenceStore
import io.github.mindzard.mythicinvasion.application.society.FactionRelationService
import io.github.mindzard.mythicinvasion.application.society.SettlementObservationCoordinator
import io.github.mindzard.mythicinvasion.application.society.SettlementObservationEngine
import io.github.mindzard.mythicinvasion.application.society.SettlementSocialCoordinator
import io.github.mindzard.mythicinvasion.application.society.SettlementSocialEngine
import io.github.mindzard.mythicinvasion.application.society.SettlementSocialStore
import io.github.mindzard.mythicinvasion.application.society.SocietyStateStore
import io.github.mindzard.mythicinvasion.application.society.VillagerRelationshipCoordinator
import io.github.mindzard.mythicinvasion.application.society.VillagerRelationshipEngine
import io.github.mindzard.mythicinvasion.application.society.VillagerRelationshipStore
import io.github.mindzard.mythicinvasion.application.society.VillagerSocietyCoordinator
import io.github.mindzard.mythicinvasion.application.society.VillagerSocietyEngine
import io.github.mindzard.mythicinvasion.application.society.VillagerSocietyStore
import io.github.mindzard.mythicinvasion.application.world.WorldIntelligenceCoordinator
import io.github.mindzard.mythicinvasion.application.world.WorldIntelligenceEngine
import io.github.mindzard.mythicinvasion.application.world.WorldStateStore
import io.github.mindzard.mythicinvasion.concurrency.CoroutineEngine
import io.github.mindzard.mythicinvasion.config.ConfigurationManager
import io.github.mindzard.mythicinvasion.infrastructure.ai.GeminiStrategyClient
import io.github.mindzard.mythicinvasion.infrastructure.paper.command.SocietyDebugCommand
import io.github.mindzard.mythicinvasion.infrastructure.paper.ecosystem.PlayerSnapshotCollector
import io.github.mindzard.mythicinvasion.infrastructure.paper.player.PlayerBehaviourListener
import io.github.mindzard.mythicinvasion.infrastructure.paper.society.SettlementObservationCollector
import io.github.mindzard.mythicinvasion.infrastructure.paper.society.VillagerObservationCollector
import io.github.mindzard.mythicinvasion.infrastructure.paper.society.VillagerRelationshipCollector
import io.github.mindzard.mythicinvasion.infrastructure.paper.world.WorldSnapshotCollector

class PluginBootstrap(
    private val plugin: MythicInvasionPlugin
) {

    fun start(): ServiceRegistry {

        val registry =
            ServiceRegistry()

        /*
         * =========================================================
         * CORE CONFIGURATION
         * =========================================================
         */

        val configurationManager =
            ConfigurationManager(
                plugin
            )

        configurationManager.load()

        registry.registerConfigurationManager(
            configurationManager
        )

        /*
         * =========================================================
         * COROUTINE ENGINE
         * =========================================================
         */

        val coroutineEngine =
            CoroutineEngine(
                plugin
            )

        registry.registerCoroutineEngine(
            coroutineEngine
        )

        /*
         * =========================================================
         * ECOSYSTEM SUBSYSTEM
         * =========================================================
         */

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
                snapshotCollector =
                    playerSnapshotCollector,
                ecosystemEngine =
                    ecosystemEngine,
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
         * =========================================================
         * PLAYER BEHAVIOUR SUBSYSTEM
         * =========================================================
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
                intelligenceStore =
                    behaviourIntelligenceStore,
                processingIntervalMillis = {
                    configurationManager
                        .behaviourProcessingIntervalMillis()
                }
            )

        registry.registerBehaviourProcessor(
            behaviourProcessor
        )

        /*
         * =========================================================
         * WORLD INTELLIGENCE SUBSYSTEM
         * =========================================================
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
                collector =
                    worldSnapshotCollector,
                engine =
                    worldIntelligenceEngine,
                stateStore =
                    worldStateStore,
                updateIntervalMillis = {
                    configurationManager
                        .worldIntelligenceUpdateIntervalMillis()
                }
            )

        registry.registerWorldIntelligenceCoordinator(
            worldIntelligenceCoordinator
        )

        /*
         * =========================================================
         * SETTLEMENT SUBSYSTEM
         * =========================================================
         */

        val settlementObservationCollector =
            SettlementObservationCollector(
                plugin
            )

        registry.registerSettlementObservationCollector(
            settlementObservationCollector
        )

        val settlementObservationEngine =
            SettlementObservationEngine()

        registry.registerSettlementObservationEngine(
            settlementObservationEngine
        )

        val societyStateStore =
            SocietyStateStore()

        registry.registerSocietyStateStore(
            societyStateStore
        )

        val factionRelationService =
            FactionRelationService()

        registry.registerFactionRelationService(
            factionRelationService
        )

        val settlementObservationCoordinator =
            SettlementObservationCoordinator(
                plugin = plugin,
                coroutineEngine = coroutineEngine,
                collector =
                    settlementObservationCollector,
                engine =
                    settlementObservationEngine,
                stateStore =
                    societyStateStore,
                updateIntervalMillis = {
                    configurationManager
                        .societyObservationIntervalMillis()
                }
            )

        registry.registerSettlementObservationCoordinator(
            settlementObservationCoordinator
        )

        /*
         * =========================================================
         * VILLAGER SOCIETY SUBSYSTEM
         * =========================================================
         */

        val villagerObservationCollector =
            VillagerObservationCollector(
                plugin
            )

        registry.registerVillagerObservationCollector(
            villagerObservationCollector
        )

        val villagerSocietyEngine =
            VillagerSocietyEngine()

        registry.registerVillagerSocietyEngine(
            villagerSocietyEngine
        )

        val villagerSocietyStore =
            VillagerSocietyStore()

        registry.registerVillagerSocietyStore(
            villagerSocietyStore
        )

        val villagerSocietyCoordinator =
            VillagerSocietyCoordinator(
                plugin = plugin,
                coroutineEngine = coroutineEngine,
                collector =
                    villagerObservationCollector,
                engine =
                    villagerSocietyEngine,
                stateStore =
                    societyStateStore,
                societyStore =
                    villagerSocietyStore,
                updateIntervalMillis = {
                    configurationManager
                        .villagerAnalysisIntervalMillis()
                }
            )

        registry.registerVillagerSocietyCoordinator(
            villagerSocietyCoordinator
        )

        /*
         * =========================================================
         * VILLAGER ↔ PLAYER RELATIONSHIP SUBSYSTEM
         * =========================================================
         */

        val villagerRelationshipEngine =
            VillagerRelationshipEngine()

        val villagerRelationshipCollector =
            VillagerRelationshipCollector(
                plugin = plugin,
                engine =
                    villagerRelationshipEngine
            )

        registry.registerVillagerRelationshipCollector(
            villagerRelationshipCollector
        )

        val villagerRelationshipStore =
            VillagerRelationshipStore()

        registry.registerVillagerRelationshipStore(
            villagerRelationshipStore
        )

        val villagerRelationshipCoordinator =
            VillagerRelationshipCoordinator(
                plugin = plugin,
                coroutineEngine = coroutineEngine,
                collector =
                    villagerRelationshipCollector,
                stateStore =
                    societyStateStore,
                relationshipStore =
                    villagerRelationshipStore,
                updateIntervalMillis = {
                    configurationManager
                        .villagerAnalysisIntervalMillis()
                }
            )

        registry.registerVillagerRelationshipCoordinator(
            villagerRelationshipCoordinator
        )

        /*
         * =========================================================
         * SETTLEMENT SOCIAL INTELLIGENCE
         * =========================================================
         */

        val settlementSocialEngine =
            SettlementSocialEngine()

        registry.registerSettlementSocialEngine(
            settlementSocialEngine
        )

        val settlementSocialStore =
            SettlementSocialStore()

        registry.registerSettlementSocialStore(
            settlementSocialStore
        )

        val settlementSocialCoordinator =
            SettlementSocialCoordinator(
                plugin = plugin,
                coroutineEngine = coroutineEngine,
                relationshipStore =
                    villagerRelationshipStore,
                engine =
                    settlementSocialEngine,
                socialStore =
                    settlementSocialStore,
                updateIntervalMillis = {
                    configurationManager
                        .societySocialIntervalMillis()
                }
            )

        registry.registerSettlementSocialCoordinator(
            settlementSocialCoordinator
        )

        /*
         * =========================================================
         * AI STRATEGY SUBSYSTEM
         * =========================================================
         *
         * AI remains optional.
         *
         * When ai.enabled=false:
         * - no Gemini client is created
         * - no API request is made
         * - no external network call occurs
         *
         * When ai.enabled=true and provider=gemini:
         * - the existing player/world/society state is assembled
         * - Gemini receives a bounded strategic context
         * - Gemini returns a high-level strategy
         *
         * Gemini does NOT directly control Minecraft entities.
         */

        if (
            configurationManager.isAiEnabled() &&
            configurationManager.aiProvider() == "gemini"
        ) {

            val aiContextAssembler =
                AiContextAssembler(
                    worldStateStore =
                        worldStateStore,
                    behaviourIntelligenceStore =
                        behaviourIntelligenceStore,
                    societyStateStore =
                        societyStateStore,
                    settlementSocialStore =
                        settlementSocialStore
                )

            registry.registerAiContextAssembler(
                aiContextAssembler
            )

            val geminiStrategyClient =
                GeminiStrategyClient(
                    plugin = plugin,
                    model =
                        configurationManager.aiModel(),
                    timeoutMillis =
                        configurationManager
                            .aiRequestTimeoutMillis()
                )

            registry.registerGeminiStrategyClient(
                geminiStrategyClient
            )

            val aiStrategyCoordinator =
                AiStrategyCoordinator(
                    plugin = plugin,
                    coroutineEngine =
                        coroutineEngine,
                    contextAssembler =
                        aiContextAssembler,
                    geminiClient =
                        geminiStrategyClient,
                    updateIntervalMillis = {
                        configurationManager
                            .aiStrategyIntervalMillis()
                    },
                    maxContextCharacters =
                        configurationManager
                            .aiMaxContextCharacters()
                )

            registry.registerAiStrategyCoordinator(
                aiStrategyCoordinator
            )

            aiStrategyCoordinator.start()

            plugin.logger.info(
                "Gemini AI strategy subsystem initialized."
            )

        } else {

            plugin.logger.info(
                "Gemini AI strategy subsystem is disabled."
            )
        }

        /*
         * =========================================================
         * MINECRAFT EVENT LISTENERS
         * =========================================================
         */

        plugin.server.pluginManager.registerEvents(
            PlayerBehaviourListener(
                buffer =
                    behaviourEventBuffer
            ),
            plugin
        )

        /*
         * =========================================================
         * DEBUG COMMAND
         * =========================================================
         */

        plugin.getCommand(
            "society"
        )?.setExecutor(
            SocietyDebugCommand(
                societyStateStore =
                    societyStateStore,
                socialStore =
                    settlementSocialStore
            )
        )

        /*
         * =========================================================
         * START ENABLED SUBSYSTEMS
         * =========================================================
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

        if (
            configurationManager
                .isSocietyEnabled()
        ) {

            settlementObservationCoordinator.start()

            villagerSocietyCoordinator.start()

            villagerRelationshipCoordinator.start()

            settlementSocialCoordinator.start()
        }

        plugin.logger.info(
            "All enabled MythicInvasion subsystems initialized."
        )

        return registry
    }
}
