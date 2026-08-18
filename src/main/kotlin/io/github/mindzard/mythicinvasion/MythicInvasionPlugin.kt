package io.github.mindzard.mythicinvasion

import io.github.mindzard.mythicinvasion.bootstrap.PluginBootstrap
import io.github.mindzard.mythicinvasion.bootstrap.ServiceRegistry
import org.bukkit.plugin.java.JavaPlugin

class MythicInvasionPlugin : JavaPlugin() {

    private lateinit var services: ServiceRegistry

    override fun onEnable() {

        logger.info(
            "=========================================="
        )

        logger.info(
            "Starting MythicInvasion..."
        )

        logger.info(
            "AI-Powered Dynamic Mythic Invasion & Ecosystem Engine"
        )

        try {

            val bootstrap =
                PluginBootstrap(this)

            services =
                bootstrap.start()

            logger.info(
                "MythicInvasion started successfully."
            )

            logger.info(
                "=========================================="
            )

        } catch (exception: Exception) {

            logger.severe(
                "MythicInvasion failed to start."
            )

            exception.printStackTrace()

            server.pluginManager.disablePlugin(
                this
            )
        }
    }

    override fun onDisable() {

        logger.info(
            "Shutting down MythicInvasion..."
        )

        if (::services.isInitialized) {

            try {

                services.behaviourProcessor.stop()

                logger.info(
                    "Behaviour processor stopped."
                )

                services.worldIntelligenceCoordinator.stop()

                logger.info(
                    "World intelligence coordinator stopped."
                )

                services.ecosystemCoordinator.stop()

                logger.info(
                    "Ecosystem coordinator stopped."
                )

                services.coroutineEngine.shutdown()

                logger.info(
                    "Coroutine engine stopped."
                )

                services.behaviourProfileStore.clear()

                services.behaviourEventBuffer.clear()

                services.behaviourIntelligenceStore.clear()

                services.worldStateStore.reset()

                logger.info(
                    "All in-memory intelligence state cleared."
                )

            } catch (exception: Exception) {

                logger.severe(
                    "Error while shutting down MythicInvasion services."
                )

                exception.printStackTrace()
            }
        }

        logger.info(
            "MythicInvasion shutdown completed."
        )
    }
}
