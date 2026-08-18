package io.github.mindzard.mythicinvasion.application.society

import io.github.mindzard.mythicinvasion.concurrency.CoroutineEngine
import io.github.mindzard.mythicinvasion.domain.society.FactionState
import io.github.mindzard.mythicinvasion.domain.society.FactionType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.bukkit.entity.Pillager
import org.bukkit.plugin.java.JavaPlugin

class PillagerFactionCoordinator(
    private val plugin: JavaPlugin,
    private val coroutineEngine: CoroutineEngine,
    private val stateStore: SocietyStateStore,
    private val updateIntervalMillis: () -> Long
) {

    companion object {

        private const val FACTION_ID =
            "pillagers"

        private const val VILLAGER_FACTION_ID =
            "villagers"
    }

    private var job: Job? = null

    fun start() {

        if (
            job != null
        ) {
            return
        }

        job =
            coroutineEngine.scope.launch {

                plugin.logger.info(
                    "Pillager faction intelligence started."
                )

                while (
                    isActive
                ) {

                    try {

                        val snapshot =
                            plugin.server
                                .scheduler
                                .callSyncMethod(
                                    plugin
                                ) {
                                    collectSnapshot()
                                }
                                .get()

                        updateFactionState(
                            snapshot
                        )

                    } catch (
                        exception: Exception
                    ) {

                        plugin.logger.warning(
                            "Pillager faction cycle failed: " +
                                "${exception.javaClass.simpleName}: " +
                                "${exception.message}"
                        )
                    }

                    delay(
                        updateIntervalMillis()
                    )
                }
            }
    }

    fun stop() {

        job?.cancel()
        job = null

        plugin.logger.info(
            "Pillager faction intelligence stopped."
        )
    }

    private fun collectSnapshot():
        PillagerFactionSnapshot {

        val settlements =
            stateStore
                .current()
                .settlements
                .values
                .toList()

        var pillagerCount =
            0

        var nearbyPillagers =
            0

        val activeWorldNames =
            mutableSetOf<String>()

        for (
            world in
            plugin.server.worlds
        ) {

            val pillagers =
                world.getEntitiesByClass(
                    Pillager::class.java
                )

            pillagerCount +=
                pillagers.size

            if (
                pillagers.isNotEmpty()
            ) {
                activeWorldNames +=
                    world.name
            }

            for (
                settlement in
                settlements
            ) {

                if (
                    settlement.worldName !=
                    world.name
                ) {
                    continue
                }

                val radius =
                    settlement.radius
                        .toDouble()
                        .coerceAtLeast(
                            1.0
                        )

                val radiusSquared =
                    radius *
                        radius

                if (
                    pillagers.any { pillager ->

                        val location =
                            pillager.location

                        val dx =
                            location.x -
                                settlement.centerX

                        val dy =
                            location.y -
                                settlement.centerY

                        val dz =
                            location.z -
                                settlement.centerZ

                        (
                            dx * dx +
                                dy * dy +
                                dz * dz
                            ) <=
                            radiusSquared
                    }
                ) {

                    nearbyPillagers++
                }
            }
        }

        val settlementCount =
            settlements.size

        val militaryStrength =
            (
                pillagerCount /
                    25.0
                )
                .coerceIn(
                    0.0,
                    1.0
                )

        val influence =
            if (
                settlementCount <= 0
            ) {
                0.0
            } else {
                (
                    nearbyPillagers.toDouble() /
                        settlementCount.toDouble()
                    )
                    .coerceIn(
                        0.0,
                        1.0
                    )
            }

        return PillagerFactionSnapshot(
            population =
                pillagerCount,

            nearbySettlementPressure =
                nearbyPillagers,

            militaryStrength =
                militaryStrength,

            influence =
                influence,

            worldCount =
                activeWorldNames.size
        )
    }

    private fun updateFactionState(
        snapshot: PillagerFactionSnapshot
    ) {

        val now =
            System.currentTimeMillis()

        val faction =
            FactionState(
                factionId =
                    FACTION_ID,

                type =
                    FactionType.PILLAGER,

                displayName =
                    "Pillagers",

                population =
                    snapshot.population,

                resources =
                    snapshot.population
                        .toDouble(),

                militaryStrength =
                    snapshot.militaryStrength,

                influence =
                    snapshot.influence,

                lastUpdatedMillis =
                    now
            )

        stateStore.upsertFaction(
            faction
        )

        if (
            plugin.config.getBoolean(
                "plugin.debug",
                false
            )
        ) {

            plugin.logger.info(
                "Pillager faction: " +
                    "population=" +
                    snapshot.population +
                    ", settlementPressure=" +
                    snapshot.nearbySettlementPressure +
                    ", strength=" +
                    "%.2f".format(
                        snapshot.militaryStrength
                    ) +
                    ", influence=" +
                    "%.2f".format(
                        snapshot.influence
                    ) +
                    ", worlds=" +
                    snapshot.worldCount
            )
        }
    }

    private data class PillagerFactionSnapshot(
        val population: Int,
        val nearbySettlementPressure: Int,
        val militaryStrength: Double,
        val influence: Double,
        val worldCount: Int
    )
}
