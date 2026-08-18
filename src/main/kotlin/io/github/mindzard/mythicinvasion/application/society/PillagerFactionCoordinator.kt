package io.github.mindzard.mythicinvasion.application.society

import io.github.mindzard.mythicinvasion.concurrency.CoroutineEngine
import io.github.mindzard.mythicinvasion.domain.society.FactionState
import io.github.mindzard.mythicinvasion.domain.society.FactionType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.bukkit.NamespacedKey
import org.bukkit.entity.Pillager
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import kotlin.math.min

class PillagerFactionCoordinator(
    private val plugin: JavaPlugin,
    private val coroutineEngine: CoroutineEngine,
    private val stateStore: SocietyStateStore,
    private val socialStore: SettlementSocialStore,
    private val strategyStore: PillagerStrategyStore,
    private val targetingEngine: PillagerSettlementTargetingEngine,
    private val updateIntervalMillis: () -> Long,
    private val assignmentRadius: Double
) {

    companion object {

        private const val FACTION_ID =
            "pillagers"

        private const val TARGET_KEY =
            "target_settlement"

        private const val MODE_KEY =
            "strategy_mode"

        private const val SCOUT_MODE =
            "SCOUT"

        private const val PRESSURE_MODE =
            "PRESSURE"
    }

    private var job: Job? = null

    private lateinit var targetSettlementKey: NamespacedKey
    private lateinit var strategyModeKey: NamespacedKey

    fun start() {

        if (
            job != null
        ) {
            return
        }

        targetSettlementKey =
            NamespacedKey(
                plugin,
                TARGET_KEY
            )

        strategyModeKey =
            NamespacedKey(
                plugin,
                MODE_KEY
            )

        job =
            coroutineEngine.scope.launch {

                plugin.logger.info(
                    "Pillager faction strategy started."
                )

                while (
                    isActive
                ) {

                    try {

                        plugin.server.scheduler
                            .callSyncMethod(
                                plugin
                            ) {
                                runCycle()
                            }
                            .get()

                    } catch (
                        exception: Exception
                    ) {

                        plugin.logger.warning(
                            "Pillager strategy cycle failed: " +
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

        if (
            ::targetSettlementKey.isInitialized
        ) {
            clearEntityStrategyTags()
        }

        strategyStore.clear()

        plugin.logger.info(
            "Pillager faction strategy stopped."
        )
    }

    private fun runCycle() {

        val settlements =
            stateStore
                .current()
                .settlements
                .values
                .toList()

        val pillagers =
            plugin.server.worlds
                .flatMap { world ->
                    world.getEntitiesByClass(
                        Pillager::class.java
                    )
                }

        updateFactionState(
            pillagerCount =
                pillagers.size
        )

        if (
            settlements.isEmpty() ||
            pillagers.isEmpty()
        ) {

            strategyStore.update(
                io.github.mindzard.mythicinvasion.domain.society.PillagerStrategyState(
                    updatedAtMillis =
                        System.currentTimeMillis()
                )
            )

            return
        }

        val socialProfiles =
            socialStore.snapshot()
                .associateBy {
                    it.settlementId
                }

        val scored =
            settlements
                .map { settlement ->

                    val nearbyPillagers =
                        countNearbyPillagers(
                            settlement,
                            pillagers
                        )

                    targetingEngine.score(
                        settlement =
                            settlement,

                        socialProfile =
                            socialProfiles[
                                settlement.settlementId
                            ],

                        pillagerCountNearSettlement =
                            nearbyPillagers
                    )
                }

        val selected =
            scored
                .maxByOrNull {
                    it.score
                }

        if (
            selected == null
        ) {
            return
        }

        val selectedSettlement =
            settlements.firstOrNull {
                it.settlementId ==
                    selected.settlementId
            }
                ?: return

        val assignments =
            assignPillagers(
                pillagers =
                    pillagers,

                settlement =
                    selectedSettlement
            )

        strategyStore.update(
            io.github.mindzard.mythicinvasion.domain.society.PillagerStrategyState(
                selectedSettlementId =
                    selected.settlementId,

                selectedSettlementName =
                    selectedSettlement.name,

                targetScore =
                    selected.score,

                targetReason =
                    selected.reason,

                assignedPillagerCount =
                    assignments.pressure,

                scoutingPillagerCount =
                    assignments.scout,

                updatedAtMillis =
                    System.currentTimeMillis()
            )
        )

        if (
            plugin.config.getBoolean(
                "plugin.debug",
                false
            )
        ) {

            plugin.logger.info(
                "Pillager target selected: " +
                    selectedSettlement.name +
                    " | score=" +
                    "%.2f".format(
                        selected.score
                    ) +
                    " | reason=" +
                    selected.reason +
                    " | pressure=" +
                    assignments.pressure +
                    " | scout=" +
                    assignments.scout
            )
        }
    }

    private fun countNearbyPillagers(
        settlement:
            io.github.mindzard.mythicinvasion
                .domain.society.SettlementState,
        pillagers:
            List<Pillager>
    ): Int {

        val radius =
            min(
                assignmentRadius,
                settlement.radius * 2.0
            )

        val radiusSquared =
            radius * radius

        return pillagers.count { pillager ->

            if (
                pillager.world.name !=
                settlement.worldName
            ) {
                return@count false
            }

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
    }

    private fun assignPillagers(
        pillagers:
            List<Pillager>,
        settlement:
            io.github.mindzard.mythicinvasion
                .domain.society.SettlementState
    ): AssignmentResult {

        val candidates =
            pillagers
                .filter {
                    it.world.name ==
                        settlement.worldName
                }
                .map { pillager ->

                    val dx =
                        pillager.location.x -
                            settlement.centerX

                    val dy =
                        pillager.location.y -
                            settlement.centerY

                    val dz =
                        pillager.location.z -
                            settlement.centerZ

                    val distanceSquared =
                        dx * dx +
                            dy * dy +
                            dz * dz

                    pillager to
                        distanceSquared
                }
                .filter {
                    it.second <=
                        assignmentRadius *
                        assignmentRadius
                }
                .sortedBy {
                    it.second
                }

        /*
         * We do not force every pillager in the world to join
         * one target. Only nearby units receive the strategy.
         */
        val selectedUnits =
            candidates.take(
                12
            )

        if (
            selectedUnits.isEmpty()
        ) {
            return AssignmentResult()
        }

        val scoutCount =
            min(
                3,
                selectedUnits.size
            )

        selectedUnits.forEachIndexed {
                index,
                pair ->

            val pillager =
                pair.first

            val mode =
                if (
                    index < scoutCount
                ) {
                    SCOUT_MODE
                } else {
                    PRESSURE_MODE
                }

            pillager
                .persistentDataContainer
                .set(
                    targetSettlementKey,
                    PersistentDataType.STRING,
                    settlement.settlementId
                )

            pillager
                .persistentDataContainer
                .set(
                    strategyModeKey,
                    PersistentDataType.STRING,
                    mode
                )
        }

        return AssignmentResult(
            pressure =
                selectedUnits.size -
                    scoutCount,

            scout =
                scoutCount
        )
    }

    private fun clearEntityStrategyTags() {

        for (
            world in
            plugin.server.worlds
        ) {

            world
                .getEntitiesByClass(
                    Pillager::class.java
                )
                .forEach { pillager ->

                    pillager
                        .persistentDataContainer
                        .remove(
                            targetSettlementKey
                        )

                    pillager
                        .persistentDataContainer
                        .remove(
                            strategyModeKey
                        )
                }
        }
    }

    private fun updateFactionState(
        pillagerCount: Int
    ) {

        val militaryStrength =
            min(
                pillagerCount /
                    25.0,
                1.0
            )

        stateStore.upsertFaction(
            FactionState(
                factionId =
                    FACTION_ID,

                type =
                    FactionType.PILLAGER,

                displayName =
                    "Pillager Faction",

                population =
                    pillagerCount,

                resources =
                    pillagerCount.toDouble(),

                militaryStrength =
                    militaryStrength,

                influence =
                    strategyStore
                        .current()
                        .targetScore,

                lastUpdatedMillis =
                    System.currentTimeMillis()
            )
        )
    }

    private data class AssignmentResult(
        val pressure: Int = 0,
        val scout: Int = 0
    )
}
