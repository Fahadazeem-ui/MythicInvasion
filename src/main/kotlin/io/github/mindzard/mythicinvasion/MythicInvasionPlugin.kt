package io.github.mindzard.mythicinvasion

import org.bukkit.plugin.java.JavaPlugin

class MythicInvasionPlugin : JavaPlugin() {

    override fun onEnable() {
        logger.info("========================================")
        logger.info("MythicInvasion is starting...")
        logger.info("AI ecosystem engine foundation loaded.")
        logger.info("Minecraft/Paper target: 1.21.11")
        logger.info("========================================")
    }

    override fun onDisable() {
        logger.info("========================================")
        logger.info("MythicInvasion is shutting down...")
        logger.info("All active systems are being stopped.")
        logger.info("========================================")
    }
}
