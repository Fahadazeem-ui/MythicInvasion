package io.github.mindzard.mythicinvasion.bootstrap

import io.github.mindzard.mythicinvasion.application.ai.AiContextAssembler
import io.github.mindzard.mythicinvasion.application.ai.AiStrategyCoordinator
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
import io.github.mindzard.mythicinvasion.infrastructure.paper.ecosystem.PlayerSnapshotCollector
import io.github.mindzard.mythicinvasion.infrastructure.paper.society.SettlementObservationCollector
import io.github.mindzard.mythicinvasion.infrastructure.paper.society.VillagerObservationCollector
import io.github.mindzard.mythicinvasion.infrastructure.paper.society.VillagerRelationshipCollector
import io.github.mindzard.mythicinvasion.infrastructure.paper.world.WorldSnapshotCollector

class ServiceRegistry {

    /*
     * ============================================================
     * CORE
     * ============================================================
     */

    lateinit var configurationManager: ConfigurationManager
        private set

    lateinit var coroutineEngine: CoroutineEngine
        private set

    /*
     * ============================================================
     * ECOSYSTEM
     * ============================================================
     */

    lateinit var playerSnapshotCollector: PlayerSnapshotCollector
        private set

    lateinit var ecosystemEngine: EcosystemEngine
        private set

    lateinit var ecosystemCoordinator: EcosystemCoordinator
        private set

    /*
     * ============================================================
     * PLAYER BEHAVIOUR
     * ============================================================
     */

    lateinit var behaviourEventBuffer: BehaviourEventBuffer
        private set

    lateinit var behaviourProfileStore: BehaviourProfileStore
        private set

    lateinit var behaviourFeatureEngine: BehaviourFeatureEngine
        private set

    lateinit var behaviourProcessor: BehaviourProcessor
        private set

    lateinit var behaviourIntelligenceEngine: BehaviourIntelligenceEngine
        private set

    lateinit var behaviourIntelligenceStore: BehaviourIntelligenceStore
        private set

    /*
     * ============================================================
     * WORLD INTELLIGENCE
     * ============================================================
     */

    lateinit var worldSnapshotCollector: WorldSnapshotCollector
        private set

    lateinit var worldIntelligenceEngine: WorldIntelligenceEngine
        private set

    lateinit var worldStateStore: WorldStateStore
        private set

    lateinit var worldIntelligenceCoordinator: WorldIntelligenceCoordinator
        private set

    /*
     * ============================================================
     * SETTLEMENT / SOCIETY
     * ============================================================
     */

    lateinit var settlementObservationCollector: SettlementObservationCollector
        private set

    lateinit var settlementObservationEngine: SettlementObservationEngine
        private set

    lateinit var societyStateStore: SocietyStateStore
        private set

    lateinit var factionRelationService: FactionRelationService
        private set

    lateinit var settlementObservationCoordinator: SettlementObservationCoordinator
        private set

    /*
     * ============================================================
     * VILLAGER SOCIETY
     * ============================================================
     */

    lateinit var villagerObservationCollector: VillagerObservationCollector
        private set

    lateinit var villagerSocietyEngine: VillagerSocietyEngine
        private set

    lateinit var villagerSocietyStore: VillagerSocietyStore
        private set

    lateinit var villagerSocietyCoordinator: VillagerSocietyCoordinator
        private set

    /*
     * ============================================================
     * VILLAGER ↔ PLAYER RELATIONSHIPS
     * ============================================================
     */

    lateinit var villagerRelationshipCollector: VillagerRelationshipCollector
        private set

    lateinit var villagerRelationshipStore: VillagerRelationshipStore
        private set

    lateinit var villagerRelationshipCoordinator: VillagerRelationshipCoordinator
        private set

    /*
     * ============================================================
     * SETTLEMENT SOCIAL INTELLIGENCE
     * ============================================================
     */

    lateinit var settlementSocialEngine: SettlementSocialEngine
        private set

    lateinit var settlementSocialStore: SettlementSocialStore
        private set

    lateinit var settlementSocialCoordinator: SettlementSocialCoordinator
        private set

    /*
     * ============================================================
     * AI STRATEGY LAYER
     * ============================================================
     *
     * These services are only initialized when AI is enabled.
     */

    lateinit var aiContextAssembler: AiContextAssembler
        private set

    lateinit var geminiStrategyClient: GeminiStrategyClient
        private set

    lateinit var aiStrategyCoordinator: AiStrategyCoordinator
        private set

    /*
     * ============================================================
     * REGISTRATION
     * ============================================================
     */

    fun registerConfigurationManager(
        manager: ConfigurationManager
    ) {
        configurationManager = manager
    }

    fun registerCoroutineEngine(
        engine: CoroutineEngine
    ) {
        coroutineEngine = engine
    }

    /*
     * ============================================================
     * ECOSYSTEM REGISTRATION
     * ============================================================
     */

    fun registerPlayerSnapshotCollector(
        collector: PlayerSnapshotCollector
    ) {
        playerSnapshotCollector = collector
    }

    fun registerEcosystemEngine(
        engine: EcosystemEngine
    ) {
        ecosystemEngine = engine
    }

    fun registerEcosystemCoordinator(
        coordinator: EcosystemCoordinator
    ) {
        ecosystemCoordinator = coordinator
    }

    /*
     * ============================================================
     * BEHAVIOUR REGISTRATION
     * ============================================================
     */

    fun registerBehaviourEventBuffer(
        buffer: BehaviourEventBuffer
    ) {
        behaviourEventBuffer = buffer
    }

    fun registerBehaviourProfileStore(
        store: BehaviourProfileStore
    ) {
        behaviourProfileStore = store
    }

    fun registerBehaviourFeatureEngine(
        engine: BehaviourFeatureEngine
    ) {
        behaviourFeatureEngine = engine
    }

    fun registerBehaviourProcessor(
        processor: BehaviourProcessor
    ) {
        behaviourProcessor = processor
    }

    fun registerBehaviourIntelligenceEngine(
        engine: BehaviourIntelligenceEngine
    ) {
        behaviourIntelligenceEngine = engine
    }

    fun registerBehaviourIntelligenceStore(
        store: BehaviourIntelligenceStore
    ) {
        behaviourIntelligenceStore = store
    }

    /*
     * ============================================================
     * WORLD INTELLIGENCE REGISTRATION
     * ============================================================
     */

    fun registerWorldSnapshotCollector(
        collector: WorldSnapshotCollector
    ) {
        worldSnapshotCollector = collector
    }

    fun registerWorldIntelligenceEngine(
        engine: WorldIntelligenceEngine
    ) {
        worldIntelligenceEngine = engine
    }

    fun registerWorldStateStore(
        store: WorldStateStore
    ) {
        worldStateStore = store
    }

    fun registerWorldIntelligenceCoordinator(
        coordinator: WorldIntelligenceCoordinator
    ) {
        worldIntelligenceCoordinator = coordinator
    }

    /*
     * ============================================================
     * SETTLEMENT REGISTRATION
     * ============================================================
     */

    fun registerSettlementObservationCollector(
        collector: SettlementObservationCollector
    ) {
        settlementObservationCollector = collector
    }

    fun registerSettlementObservationEngine(
        engine: SettlementObservationEngine
    ) {
        settlementObservationEngine = engine
    }

    fun registerSocietyStateStore(
        store: SocietyStateStore
    ) {
        societyStateStore = store
    }

    fun registerFactionRelationService(
        service: FactionRelationService
    ) {
        factionRelationService = service
    }

    fun registerSettlementObservationCoordinator(
        coordinator: SettlementObservationCoordinator
    ) {
        settlementObservationCoordinator = coordinator
    }

    /*
     * ============================================================
     * VILLAGER SOCIETY REGISTRATION
     * ============================================================
     */

    fun registerVillagerObservationCollector(
        collector: VillagerObservationCollector
    ) {
        villagerObservationCollector = collector
    }

    fun registerVillagerSocietyEngine(
        engine: VillagerSocietyEngine
    ) {
        villagerSocietyEngine = engine
    }

    fun registerVillagerSocietyStore(
        store: VillagerSocietyStore
    ) {
        villagerSocietyStore = store
    }

    fun registerVillagerSocietyCoordinator(
        coordinator: VillagerSocietyCoordinator
    ) {
        villagerSocietyCoordinator = coordinator
    }

    /*
     * ============================================================
     * VILLAGER ↔ PLAYER REGISTRATION
     * ============================================================
     */

    fun registerVillagerRelationshipCollector(
        collector: VillagerRelationshipCollector
    ) {
        villagerRelationshipCollector = collector
    }

    fun registerVillagerRelationshipStore(
        store: VillagerRelationshipStore
    ) {
        villagerRelationshipStore = store
    }

    fun registerVillagerRelationshipCoordinator(
        coordinator: VillagerRelationshipCoordinator
    ) {
        villagerRelationshipCoordinator = coordinator
    }

    /*
     * ============================================================
     * SETTLEMENT SOCIAL REGISTRATION
     * ============================================================
     */

    fun registerSettlementSocialEngine(
        engine: SettlementSocialEngine
    ) {
        settlementSocialEngine = engine
    }

    fun registerSettlementSocialStore(
        store: SettlementSocialStore
    ) {
        settlementSocialStore = store
    }

    fun registerSettlementSocialCoordinator(
        coordinator: SettlementSocialCoordinator
    ) {
        settlementSocialCoordinator = coordinator
    }

    /*
     * ============================================================
     * AI REGISTRATION
     * ============================================================
     */

    fun registerAiContextAssembler(
        assembler: AiContextAssembler
    ) {
        aiContextAssembler = assembler
    }

    fun registerGeminiStrategyClient(
        client: GeminiStrategyClient
    ) {
        geminiStrategyClient = client
    }

    fun registerAiStrategyCoordinator(
        coordinator: AiStrategyCoordinator
    ) {
        aiStrategyCoordinator = coordinator
    }
}
