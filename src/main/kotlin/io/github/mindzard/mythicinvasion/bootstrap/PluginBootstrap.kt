package io.github.mindzard.mythicinvasion.bootstrap

import io.github.mindzard.mythicinvasion.MythicInvasionPlugin

class PluginBootstrap(
    private val plugin: MythicInvasionPlugin
) {

    private var started: Boolean = false

    fun start() {
        check(!started) {
            "PluginBootstrap has already been started."
        }

        plugin.logger.info("Starting MythicInvasion bootstrap...")

        started = true

        plugin.logger.info("MythicInvasion bootstrap started successfully.")
    }

    fun stop() {
        if (!started) {
            return
        }

        plugin.logger.info("Stopping MythicInvasion bootstrap...")

        started = false

        plugin.logger.info("MythicInvasion bootstrap stopped successfully.")
    }

    fun isStarted(): Boolean = started
}
