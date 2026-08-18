package io.github.mindzard.mythicinvasion.bootstrap

import io.github.mindzard.mythicinvasion.application.behaviour.BehaviourEventBuffer
import io.github.mindzard.mythicinvasion.application.behaviour.BehaviourProcessor
import io.github.mindzard.mythicinvasion.application.behaviour.BehaviourProfileStore
import io.github.mindzard.mythicinvasion.application.ecosystem.EcosystemCoordinator
import io.github.mindzard.mythicinvasion.application.ecosystem.EcosystemEngine
import io.github.mindzard.mythicinvasion.concurrency.CoroutineEngine
import io.github.mindzard.mythicinvasion.config.ConfigurationManager
import io.github.mindzard.mythicinvasion.infrastructure.paper.ecosystem.PlayerSnapshotCollector

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

    lateinit var behaviourProcessor: BehaviourProcessor
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

    fun registerBehaviourProcessor(
        processor: BehaviourProcessor
    ) {
        behaviourProcessor = processor
    }
}
