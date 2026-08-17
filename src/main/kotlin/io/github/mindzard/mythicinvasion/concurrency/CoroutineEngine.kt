package io.github.mindzard.mythicinvasion.concurrency

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.bukkit.plugin.java.JavaPlugin

class CoroutineEngine(
    private val plugin: JavaPlugin
) {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        plugin.logger.severe(
            "Unhandled asynchronous exception in MythicInvasion: ${throwable.message}"
        )

        throwable.printStackTrace()
    }

    private val supervisorJob = SupervisorJob()

    val scope: CoroutineScope = CoroutineScope(
        supervisorJob +
            Dispatchers.Default +
            exceptionHandler
    )

    fun shutdown() {
        supervisorJob.cancel()
    }
}
