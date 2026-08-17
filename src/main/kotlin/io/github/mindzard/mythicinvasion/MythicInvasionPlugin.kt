package io.github.mindzard.mythicinvasion

import io.github.mindzard.mythicinvasion.bootstrap.PluginBootstrap
import io.github.mindzard.mythicinvasion.bootstrap.ServiceRegistry
import org.bukkit.plugin.java.JavaPlugin

class MythicInvasionPlugin : JavaPlugin() {

    lateinit var services: ServiceRegistry
        private set

    override fun onEnable() {
        logger.info("========================================")
        logger.info("Starting MythicInvasion...")
        logger.info("Minecraft/Paper target: 1.21.11")
        logger.info("========================================")

        try {
            services = PluginBootstrap(this).start()

            logger.info("MythicInvasion foundation initialized successfully.")
            logger.info("Ecosystem engine foundation is ready.")
        } catch (exception: Exception) {
            logger.severe(
                "Failed to initialize MythicInvasion."
            )

            exception.printStackTrace()

            server.pluginManager.disablePlugin(this)
        }
    }

    override fun onDisable() {
        logger.info("Shutting down MythicInvasion...")

        if (::services.isInitialized) {
            try {
                services.coroutineEngine.shutdown()
                logger.info("Coroutine engine stopped.")
            } catch (exception: Exception) {
                logger.severe(
                    "Error while shutting down MythicInvasion services."
                )

                exception.printStackTrace()
            }
        }

        logger.info("MythicInvasion shutdown completed.")
    }
}
