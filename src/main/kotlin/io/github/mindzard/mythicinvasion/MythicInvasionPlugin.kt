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

            /*
             * Disable the plugin if a critical startup
             * dependency fails.
             */
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
                 * Stop behaviour processing first.
                 *
                 * This also drains the remaining in-memory
                 * behaviour events into the profile store.
                 */
                services.behaviourProcessor.stop()

                logger.info(
                    "Behaviour processor stopped."
                )

                /*
                 * Stop the ecosystem coordinator so it
                 * cannot schedule any new ecosystem work.
                 */
                services.ecosystemCoordinator.stop()

                logger.info(
                    "Ecosystem coordinator stopped."
                )

                /*
                 * Stop all plugin-owned coroutine work.
                 */
                services.coroutineEngine.shutdown()

                logger.info(
                    "Coroutine engine stopped."
                )

                /*
                 * Clear in-memory behaviour data.
                 *
                 * We do not keep references to Bukkit players,
                 * worlds, or other server objects here.
                 */
                services.behaviourProfileStore.clear()

                services.behaviourEventBuffer.clear()

                logger.info(
                    "Behaviour memory cleared."
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
