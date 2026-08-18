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

            services = bootstrap.start()

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

            server.pluginManager.disablePlugin(this)
        }
    }

    override fun onDisable() {

        logger.info(
            "Shutting down MythicInvasion..."
        )

        if (::services.isInitialized) {

            try {

                /*
                 * Stop behaviour processing first so that pending
                 * in-memory observations can be consumed.
                 */
                services.behaviourProcessor.stop()

                logger.info(
                    "Behaviour processor stopped."
                )

                /*
                 * Stop ecosystem processing.
                 */
                services.ecosystemCoordinator.stop()

                logger.info(
                    "Ecosystem coordinator stopped."
                )

                /*
                 * Stop plugin-owned asynchronous work.
                 */
                services.coroutineEngine.shutdown()

                logger.info(
                    "Coroutine engine stopped."
                )

                /*
                 * Clear behaviour data.
                 */
                services.behaviourProfileStore.clear()

                services.behaviourEventBuffer.clear()

                /*
                 * Clear AI-facing intelligence data.
                 */
                services.behaviourIntelligenceStore.clear()

                logger.info(
                    "Behaviour and intelligence memory cleared."
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
