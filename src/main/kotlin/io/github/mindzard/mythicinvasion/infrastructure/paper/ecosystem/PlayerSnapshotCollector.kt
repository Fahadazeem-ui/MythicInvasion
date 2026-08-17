package io.github.mindzard.mythicinvasion.infrastructure.paper.ecosystem

import io.github.mindzard.mythicinvasion.domain.player.PlayerSnapshot
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class PlayerSnapshotCollector(
    private val plugin: JavaPlugin
) {

    fun collect(): List<PlayerSnapshot> {
        return Bukkit.getOnlinePlayers()
            .map { player ->
                val location = player.location

                PlayerSnapshot(
                    playerId = player.uniqueId,
                    name = player.name,
                    worldName = player.world.name,
                    x = location.x,
                    y = location.y,
                    z = location.z,
                    health = player.health,
                    level = player.level,
                    isSneaking = player.isSneaking,
                    isSprinting = player.isSprinting
                )
            }
    }
}
