package io.github.mindzard.mythicinvasion.infrastructure.paper.society

import io.github.mindzard.mythicinvasion.domain.society.PlayerVillagerRelationship
import io.github.mindzard.mythicinvasion.domain.society.SettlementState
import io.github.mindzard.mythicinvasion.application.society.VillagerRelationshipEngine
import org.bukkit.entity.Villager
import org.bukkit.plugin.java.JavaPlugin

class VillagerRelationshipCollector(
    private val plugin: JavaPlugin,
    private val engine: VillagerRelationshipEngine
) {

    fun collect(
        settlements: Collection<SettlementState>
    ): List<PlayerVillagerRelationship> {

        if (settlements.isEmpty()) {
            return emptyList()
        }

        val nowMillis =
            System.currentTimeMillis()

        val relationships =
            mutableListOf<PlayerVillagerRelationship>()

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
                    findSettlement(
                        worldName =
                            world.name,

                        x =
                            location.blockX,

                        y =
                            location.blockY,

                        z =
                            location.blockZ,

                        settlements =
                            settlements
                    )
                        ?: continue

                val reputations =
                    villager.reputations

                for (
                    entry in reputations
                ) {

                    val playerId =
                        entry.key

                    val reputation =
                        entry.value

                    relationships.add(
                        engine.calculate(
                            villagerId =
                                villager.uniqueId,

                            playerId =
                                playerId,

                            settlementId =
                                settlement.settlementId,

                            reputation =
                                reputation,

                            updatedAtMillis =
                                nowMillis
                        )
                    )
                }
            }
        }

        return relationships
    }

    private fun findSettlement(
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

                settlement to
                    (
                        dx * dx +
                            dy * dy +
                            dz * dz
                        )
            }
            .filter { (settlement, distanceSquared) ->
                distanceSquared <=
                    (
                        settlement.radius *
                            settlement.radius
                        )
            }
            .minByOrNull {
                it.second
            }
            ?.first
    }
}
