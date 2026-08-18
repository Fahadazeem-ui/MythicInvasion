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

            services =
                PluginBootstrap(
                    this
                ).start()

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

        if (
            ::services.isInitialized
        ) {

            try {

                /*
                 * Stop AI first so no new strategic request can
                 * begin during the rest of shutdown.
                 */
                if (
                    services
                        .isAiStrategyCoordinatorInitialized()
                ) {

                    services
                        .aiStrategyCoordinator
                        .stop()

                    logger.info(
                        "AI strategy coordinator stopped."
                    )
                }

                services.behaviourProcessor.stop()

                logger.info(
                    "Behaviour processor stopped."
                )

                services.settlementSocialCoordinator.stop()

                services.villagerRelationshipCoordinator.stop()

                services.villagerSocietyCoordinator.stop()

                services.settlementObservationCoordinator.stop()

                services.worldIntelligenceCoordinator.stop()

                services.ecosystemCoordinator.stop()

                services.coroutineEngine.shutdown()

                services.behaviourProfileStore.clear()

                services.behaviourEventBuffer.clear()

                services.behaviourIntelligenceStore.clear()

                services.villagerRelationshipStore.clear()

                services.villagerSocietyStore.clear()

                services.settlementSocialStore.clear()

                services.societyStateStore.reset()

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
