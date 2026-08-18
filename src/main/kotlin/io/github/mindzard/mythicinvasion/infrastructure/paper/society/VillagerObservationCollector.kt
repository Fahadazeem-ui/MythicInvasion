package io.github.mindzard.mythicinvasion.infrastructure.paper.society

import io.github.mindzard.mythicinvasion.domain.society.SettlementState
import io.github.mindzard.mythicinvasion.domain.society.VillagerIdentitySnapshot
import org.bukkit.entity.Villager
import org.bukkit.plugin.java.JavaPlugin

class VillagerObservationCollector(
    private val plugin: JavaPlugin
) {

    fun collect(
        settlements: Collection<SettlementState>
    ): List<VillagerIdentitySnapshot> {

        if (settlements.isEmpty()) {
            return emptyList()
        }

        val timestampMillis =
            System.currentTimeMillis()

        val snapshots =
            mutableListOf<VillagerIdentitySnapshot>()

        for (world in plugin.server.worlds) {

            for (entity in world.entities) {

                val villager =
                    entity as? Villager
                        ?: continue

                if (villager.isDead) {
                    continue
                }

                val location =
                    villager.location

                val settlement =
                    findContainingSettlement(
                        worldName = world.name,
                        x = location.blockX,
                        y = location.blockY,
                        z = location.blockZ,
                        settlements = settlements
                    )
                        ?: continue

                snapshots.add(
                    VillagerIdentitySnapshot(
                        villagerId =
                            villager.uniqueId,

                        settlementId =
                            settlement.settlementId,

                        worldName =
                            world.name,

                        x =
                            location.blockX,

                        y =
                            location.blockY,

                        z =
                            location.blockZ,

                        profession =
                            villager
                                .profession
                                .toString(),

                        villagerType =
                            villager
                                .villagerType
                                .toString(),

                        villagerLevel =
                            villager
                                .villagerLevel,

                        villagerExperience =
                            villager
                                .villagerExperience,

                        isAdult =
                            villager
                                .isAdult,

                        knownPlayerCount =
                            villager
                                .reputations
                                .size,

                        timestampMillis =
                            timestampMillis
                    )
                )
            }
        }

        return snapshots
    }

    private fun findContainingSettlement(
        worldName: String,
        x: Int,
        y: Int,
        z: Int,
        settlements: Collection<SettlementState>
    ): SettlementState? {

        return settlements
            .asSequence()
            .filter {
                it.worldName == worldName
            }
            .map { settlement ->

                val dx =
                    (
                        x -
                            settlement.centerX
                        ).toDouble()

                val dy =
                    (
                        y -
                            settlement.centerY
                        ).toDouble()

                val dz =
                    (
                        z -
                            settlement.centerZ
                        ).toDouble()

                val distanceSquared =
                    dx * dx +
                        dy * dy +
                        dz * dz

                settlement to
                    distanceSquared
            }
            .filter { (settlement, distanceSquared) ->
                distanceSquared <=
                    (
                        settlement.radius *
                            settlement.radius
                        )
            }
            .minByOrNull { (_, distanceSquared) ->
                distanceSquared
            }
            ?.first
    }
}
