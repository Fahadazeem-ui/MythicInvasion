package io.github.mindzard.mythicinvasion.infrastructure.paper.world

import io.github.mindzard.mythicinvasion.domain.world.WorldPopulationSnapshot
import io.github.mindzard.mythicinvasion.domain.world.WorldSnapshot
import org.bukkit.entity.Animals
import org.bukkit.entity.IronGolem
import org.bukkit.entity.Monster
import org.bukkit.entity.Pillager
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import org.bukkit.plugin.java.JavaPlugin

class WorldSnapshotCollector(
    private val plugin: JavaPlugin
) {

    fun collect(): WorldSnapshot {

        val timestampMillis =
            System.currentTimeMillis()

        val populations =
            plugin.server.worlds.map { world ->

                var playerCount = 0
                var villagerCount = 0
                var pillagerCount = 0
                var hostileMobCount = 0
                var passiveAnimalCount = 0
                var ironGolemCount = 0

                /*
                 * World/entity access happens on the Minecraft
                 * server thread. This collector is only called
                 * through WorldIntelligenceCoordinator's
                 * callSyncMethod().
                 */
                for (entity in world.entities) {

                    when (entity) {

                        is Player -> {
                            playerCount++
                        }

                        is Villager -> {
                            villagerCount++
                        }

                        is Pillager -> {
                            pillagerCount++
                        }

                        is IronGolem -> {
                            ironGolemCount++
                        }

                        is Monster -> {
                            hostileMobCount++
                        }

                        is Animals -> {
                            passiveAnimalCount++
                        }
                    }
                }

                WorldPopulationSnapshot(
                    worldName = world.name,
                    playerCount = playerCount,
                    villagerCount = villagerCount,
                    pillagerCount = pillagerCount,
                    hostileMobCount = hostileMobCount,
                    passiveAnimalCount = passiveAnimalCount,
                    ironGolemCount = ironGolemCount,
                    timestampMillis = timestampMillis
                )
            }

        val referenceWorld =
            plugin.server.worlds.firstOrNull()

        return WorldSnapshot(
            timestampMillis = timestampMillis,
            dayTime =
                referenceWorld?.time ?: 0L,
            isDay =
                referenceWorld?.isDayTime() ?: true,
            isRaining =
                referenceWorld?.hasStorm() ?: false,
            populations = populations
        )
    }
}
