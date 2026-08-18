package io.github.mindzard.mythicinvasion.bootstrap

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

class ServiceRegistry {

    lateinit var configurationManager: ConfigurationManager
        private set

    lateinit var coroutineEngine: CoroutineEngine
        private set

    lateinit var playerSnapshotCollector: PlayerSnapshotCollector
        private set

    lateinit var ecosystemEngine: EcosystemEngine
        private set

    lateinit var ecosystemCoordinator: EcosystemCoordinator
        private set

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

    lateinit var worldSnapshotCollector: WorldSnapshotCollector
        private set

    lateinit var worldIntelligenceEngine: WorldIntelligenceEngine
        private set

    lateinit var worldStateStore: WorldStateStore
        private set

    lateinit var worldIntelligenceCoordinator: WorldIntelligenceCoordinator
        private set

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
}
