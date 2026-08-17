package io.github.mindzard.mythicinvasion.bootstrap

import io.github.mindzard.mythicinvasion.concurrency.CoroutineEngine
import io.github.mindzard.mythicinvasion.config.ConfigurationManager

class ServiceRegistry {

    lateinit var configurationManager: ConfigurationManager
        private set

    lateinit var coroutineEngine: CoroutineEngine
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
}
