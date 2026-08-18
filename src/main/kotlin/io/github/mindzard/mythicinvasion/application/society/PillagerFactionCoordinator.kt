package io.github.mindzard.mythicinvasion.application.society

import io.github.mindzard.mythicinvasion.concurrency.CoroutineEngine
import io.github.mindzard.mythicinvasion.domain.society.FactionState
import io.github.mindzard.mythicinvasion.domain.society.FactionType
import io.github.mindzard.mythicinvasion.domain.society.PillagerStrategyState
import io.github.mindzard.mythicinvasion.domain.society.SettlementState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Pillager
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

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

        private const val PHASE_KEY =
            "strategy_phase"

        private const val ASSIGNED_AT_KEY =
            "strategy_assigned_at"

        private const val SCOUT_MODE =
            "SCOUT"

        private const val PRESSURE_MODE =
            "PRESSURE"

        private const val SCOUT_PHASE =
            "SCOUTING"

        private const val PRESSURE_PHASE =
            "PRESSURE"

        private const val REGROUP_PHASE =
            "REGROUPING"

        private const val TARGET_STICKINESS =
            0.10

        private const val RETARGET_INTERVAL_MILLIS =
            30_000L

        private const val MAX_ASSIGNED_UNITS =
            12

        private const val MAX_SCOUT_UNITS =
            3

        private const val APPROACH_DISTANCE =
            18.0

        private const val SCOUT_RING_DISTANCE =
            32.0

        private const val SCOUT_SPEED =
            0.75

        private const val PRESSURE_SPEED =
            0.90

        private const val REGROUP_SPEED =
            0.85
    }

    private var job: Job? = null

    private lateinit var targetSettlementKey: NamespacedKey
    private lateinit var strategyModeKey: NamespacedKey
    private lateinit var strategyPhaseKey: NamespacedKey
    private lateinit var assignedAtKey: NamespacedKey

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

        strategyPhaseKey =
            NamespacedKey(
                plugin,
                PHASE_KEY
            )

        assignedAtKey =
            NamespacedKey(
                plugin,
                ASSIGNED_AT_KEY
            )

        job =
            coroutineEngine.scope.launch {

                plugin.logger.info(
                    "Pillager faction intelligence started."
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
            "Pillager faction intelligence stopped."
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
                .filter {
                    !it.isDead
                }

        updateFactionState(
            pillagerCount =
                pillagers.size
        )

        if (
            settlements.isEmpty()
        ) {

            clearEntityStrategyTags()

            strategyStore.update(
                PillagerStrategyState(
                    targetReason =
                        "No settlements detected.",
                    updatedAtMillis =
                        System.currentTimeMillis()
                )
            )

            return
        }

        if (
            pillagers.isEmpty()
        ) {

            strategyStore.update(
                PillagerStrategyState(
                    targetReason =
                        "No active pillagers available.",
                    updatedAtMillis =
                        System.currentTimeMillis()
                )
            )

            return
        }

        val socialProfiles =
            socialStore
                .snapshot()
                .associateBy {
                    it.settlementId
                }

        val scoredSettlements =
            settlements
                .map { settlement ->

                    val nearbyPillagers =
                        countNearbyPillagers(
                            settlement =
                                settlement,
                            pillagers =
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
                .sortedByDescending {
                    it.score
                }

        val selectedScore =
            chooseStableTarget(
                scoredSettlements =
                    scoredSettlements
            )

        if (
            selectedScore == null
        ) {

            clearEntityStrategyTags()

            strategyStore.update(
                PillagerStrategyState(
                    targetReason =
                        "No viable strategic target.",
                    updatedAtMillis =
                        System.currentTimeMillis()
                )
            )

            return
        }

        val selectedSettlement =
            settlements.firstOrNull {
                it.settlementId ==
                    selectedScore.settlementId
            }
                ?: return

        val previousState =
            strategyStore.current()

        val targetChanged =
            previousState.selectedSettlementId !=
                selectedSettlement.settlementId

        val assignment =
            assignAndCommandUnits(
                pillagers =
                    pillagers,
                settlement =
                    selectedSettlement,
                targetScore =
                    selectedScore.score
            )

        strategyStore.update(
            PillagerStrategyState(
                selectedSettlementId =
                    selectedSettlement.settlementId,

                selectedSettlementName =
                    selectedSettlement.name,

                targetScore =
                    selectedScore.score,

                targetReason =
                    selectedScore.reason,

                assignedPillagerCount =
                    assignment.pressureUnits,

                scoutingPillagerCount =
                    assignment.scoutUnits,

                updatedAtMillis =
                    System.currentTimeMillis()
            )
        )

        if (
            targetChanged &&
            debugEnabled()
        ) {

            plugin.logger.info(
                "Pillager faction target changed to " +
                    selectedSettlement.name +
                    " | score=" +
                    "%.2f".format(
                        selectedScore.score
                    ) +
                    " | reason=" +
                    selectedScore.reason +
                    " | scouts=" +
                    assignment.scoutUnits +
                    " | pressure=" +
                    assignment.pressureUnits
            )
        }
    }

    private fun chooseStableTarget(
        scoredSettlements:
            List<PillagerSettlementTargetScore>
    ): PillagerSettlementTargetScore? {

        val best =
            scoredSettlements.firstOrNull()
                ?: return null

        val currentId =
            strategyStore.current()
                .selectedSettlementId
                ?: return best

        val current =
            scoredSettlements
                .firstOrNull {
                    it.settlementId ==
                        currentId
                }
                ?: return best

        val targetAge =
            System.currentTimeMillis() -
                strategyStore.current()
                    .updatedAtMillis

        if (
            targetAge >=
                RETARGET_INTERVAL_MILLIS
        ) {
            return best
        }

        if (
            best.settlementId ==
                current.settlementId
        ) {
            return current
        }

        return if (
            best.score -
                current.score >=
                TARGET_STICKINESS
        ) {
            best
        } else {
            current
        }
    }

    private fun assignAndCommandUnits(
        pillagers:
            List<Pillager>,
        settlement:
            SettlementState,
        targetScore: Double
    ): AssignmentResult {

        val nearby =
            pillagers
                .filter {
                    it.world.name ==
                        settlement.worldName
                }
                .map { pillager ->

                    val distance =
                        distanceSquared(
                            pillager.location,
                            settlementCenter(
                                settlement
                            )
                        )

                    pillager to
                        distance
                }
                .filter {
                    it.second <=
                        assignmentRadius *
                        assignmentRadius
                }
                .sortedBy {
                    it.second
                }

        val selected =
            nearby.take(
                MAX_ASSIGNED_UNITS
            )

        if (
            selected.isEmpty()
        ) {

            clearEntityStrategyTagsForOthers(
                selectedIds =
                    emptySet(),
                allPillagers =
                    pillagers
            )

            return AssignmentResult()
        }

        val scoutCount =
            min(
                MAX_SCOUT_UNITS,
                selected.size
            )

        val scoutIds =
            selected
                .take(
                    scoutCount
                )
                .map {
                    it.first.uniqueId
                }
                .toSet()

        val assignedIds =
            selected
                .map {
                    it.first.uniqueId
                }
                .toSet()

        var scoutUnits =
            0

        var pressureUnits =
            0

        selected.forEach { pair ->

            val pillager =
                pair.first

            val scout =
                pillager.uniqueId in
                    scoutIds

            val mode =
                if (
                    scout
                ) {
                    SCOUT_MODE
                } else {
                    PRESSURE_MODE
                }

            val phase =
                when {

                    scout ->
                        SCOUT_PHASE

                    targetScore >=
                        0.65 ->
                        PRESSURE_PHASE

                    else ->
                        REGROUP_PHASE
                }

            writeStrategyMemory(
                pillager =
                    pillager,
                settlementId =
                    settlement.settlementId,
                mode =
                    mode,
                phase =
                    phase
            )

            when (
                phase
            ) {

                SCOUT_PHASE -> {

                    scoutUnits++

                    executeScoutBehaviour(
                        pillager =
                            pillager,
                        settlement =
                            settlement
                    )
                }

                PRESSURE_PHASE -> {

                    pressureUnits++

                    executePressureBehaviour(
                        pillager =
                            pillager,
                        settlement =
                            settlement
                    )
                }

                REGROUP_PHASE -> {

                    executeRegroupBehaviour(
                        pillager =
                            pillager,
                        settlement =
                            settlement
                    )
                }
            }
        }

        clearEntityStrategyTagsForOthers(
            selectedIds =
                assignedIds,
            allPillagers =
                pillagers
        )

        return AssignmentResult(
            pressureUnits =
                pressureUnits,
            scoutUnits =
                scoutUnits
        )
    }

    private fun executeScoutBehaviour(
        pillager: Pillager,
        settlement: SettlementState
    ) {

        val scoutLocation =
            calculateScoutLocation(
                pillagerIndex =
                    stableIndex(
                        pillager.uniqueId
                    ),
                settlement =
                    settlement
            )

        if (
            distanceSquared(
                pillager.location,
                scoutLocation
            ) > 9.0
        ) {

            pillager.pathfinder.moveTo(
                scoutLocation,
                SCOUT_SPEED
            )
        }

        keepVanillaCombatAwareness(
            pillager =
                pillager,
            settlement =
                settlement
        )
    }

    private fun executePressureBehaviour(
        pillager: Pillager,
        settlement: SettlementState
    ) {

        val nearbyTarget =
            findNearbySettlementTarget(
                pillager =
                    pillager,
                settlement =
                    settlement
            )

        if (
            nearbyTarget != null
        ) {

            if (
                distanceSquared(
                    pillager.location,
                    nearbyTarget.location
                ) > 16.0
            ) {

                pillager.pathfinder.moveTo(
                    nearbyTarget.location,
                    PRESSURE_SPEED
                )
            }

            pillager.lookAt(
                nearbyTarget
            )

            return
        }

        val center =
            settlementCenter(
                settlement
            )

        if (
            distanceSquared(
                pillager.location,
                center
            ) >
            APPROACH_DISTANCE *
                APPROACH_DISTANCE
        ) {

            pillager.pathfinder.moveTo(
                center,
                PRESSURE_SPEED
            )
        }
    }

    private fun executeRegroupBehaviour(
        pillager: Pillager,
        settlement: SettlementState
    ) {

        val center =
            settlementCenter(
                settlement
            )

        if (
            distanceSquared(
                pillager.location,
                center
            ) >
            APPROACH_DISTANCE *
                APPROACH_DISTANCE
        ) {

            pillager.pathfinder.moveTo(
                center,
                REGROUP_SPEED
            )
        }

        pillager.target = null
    }

    private fun keepVanillaCombatAwareness(
        pillager: Pillager,
        settlement: SettlementState
    ) {

        val target =
            findNearbySettlementTarget(
                pillager =
                    pillager,
                settlement =
                    settlement
            )

        if (
            target != null
        ) {

            pillager.lookAt(
                target
            )
        }
    }

    private fun findNearbySettlementTarget(
        pillager: Pillager,
        settlement: SettlementState
    ): LivingEntity? {

        val center =
            settlementCenter(
                settlement
            )

        val nearby =
            pillager.world
                .getNearbyEntities(
                    pillager.location,
                    20.0,
                    12.0,
                    20.0
                )

        val player =
            nearby
                .asSequence()
                .filterIsInstance<Player>()
                .filter {
                    it.isOnline &&
                        !it.isDead
                }
                .filter {
                    distanceSquared(
                        it.location,
                        center
                    ) <=
                        (
                            settlement.radius +
                                8
                            ) *
                        (
                            settlement.radius +
                                8
                            )
                }
                .minByOrNull {
                    distanceSquared(
                        it.location,
                        pillager.location
                    )
                }

        if (
            player != null
        ) {
            return player
        }

        return nearby
            .asSequence()
            .filterIsInstance<Villager>()
            .filter {
                !it.isDead
            }
            .filter {
                distanceSquared(
                    it.location,
                    center
                ) <=
                    (
                        settlement.radius +
                            8
                        ) *
                    (
                        settlement.radius +
                            8
                        )
            }
            .minByOrNull {
                distanceSquared(
                    it.location,
                    pillager.location
                )
            }
    }

    private fun calculateScoutLocation(
        pillagerIndex: Int,
        settlement: SettlementState
    ): Location {

        val center =
            settlementCenter(
                settlement
            )

        val ringDistance =
            min(
                SCOUT_RING_DISTANCE,
                settlement.radius.toDouble()
            )

        val slot =
            pillagerIndex %
                3

        val angle =
            (
                2.0 *
                    PI /
                    3.0
                ) *
                slot

        return Location(
            center.world,
            center.x +
                cos(angle) *
                ringDistance,
            center.y,
            center.z +
                sin(angle) *
                ringDistance
        )
    }

    private fun writeStrategyMemory(
        pillager: Pillager,
        settlementId: String,
        mode: String,
        phase: String
    ) {

        pillager
            .persistentDataContainer
            .set(
                targetSettlementKey,
                PersistentDataType.STRING,
                settlementId
            )

        pillager
            .persistentDataContainer
            .set(
                strategyModeKey,
                PersistentDataType.STRING,
                mode
            )

        pillager
            .persistentDataContainer
            .set(
                strategyPhaseKey,
                PersistentDataType.STRING,
                phase
            )

        pillager
            .persistentDataContainer
            .set(
                assignedAtKey,
                PersistentDataType.LONG,
                System.currentTimeMillis()
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
                .forEach {
                    clearStrategyMemory(
                        it
                    )
                }
        }
    }

    private fun clearEntityStrategyTagsForOthers(
        selectedIds:
            Set<java.util.UUID>,
        allPillagers:
            List<Pillager>
    ) {

        allPillagers.forEach { pillager ->

            if (
                pillager.uniqueId !in
                    selectedIds
            ) {

                clearStrategyMemory(
                    pillager
                )
            }
        }
    }

    private fun clearStrategyMemory(
        pillager: Pillager
    ) {

        if (
            ::targetSettlementKey.isInitialized
        ) {

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

            pillager
                .persistentDataContainer
                .remove(
                    strategyPhaseKey
                )

            pillager
                .persistentDataContainer
                .remove(
                    assignedAtKey
                )
        }
    }

    private fun updateFactionState(
        pillagerCount: Int
    ) {

        val targetScore =
            strategyStore
                .current()
                .targetScore

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
                    targetScore,

                lastUpdatedMillis =
                    System.currentTimeMillis()
            )
        )
    }

    private fun settlementCenter(
        settlement: SettlementState
    ): Location {

        val world =
            plugin.server.getWorld(
                settlement.worldName
            )
                ?: return plugin.server
                    .worlds
                    .first()
                    .spawnLocation

        return Location(
            world,
            settlement.centerX.toDouble(),
            settlement.centerY.toDouble(),
            settlement.centerZ.toDouble()
        )
    }

    private fun countNearbyPillagers(
        settlement: SettlementState,
        pillagers: List<Pillager>
    ): Int {

        val center =
            settlementCenter(
                settlement
            )

        val radius =
            min(
                assignmentRadius,
                settlement.radius * 2.0
            )

        val radiusSquared =
            radius * radius

        return pillagers.count { pillager ->

            pillager.world.uid ==
                center.world.uid &&
                distanceSquared(
                    pillager.location,
                    center
                ) <=
                radiusSquared
        }
    }

    private fun distanceSquared(
        first: Location,
        second: Location
    ): Double {

        if (
            first.world.uid !=
                second.world.uid
        ) {
            return Double.MAX_VALUE
        }

        val dx =
            first.x -
                second.x

        val dy =
            first.y -
                second.y

        val dz =
            first.z -
                second.z

        return (
            dx * dx +
                dy * dy +
                dz * dz
            )
    }

    private fun stableIndex(
        id: java.util.UUID
    ): Int {

        return (
            id.mostSignificantBits xor
                id.leastSignificantBits
            )
            .toInt()
            .absoluteValue
    }

    private fun debugEnabled(): Boolean {

        return plugin.config.getBoolean(
            "plugin.debug",
            false
        )
    }

    private data class AssignmentResult(
        val pressureUnits: Int = 0,
        val scoutUnits: Int = 0
    )
}
