package io.github.mindzard.mythicinvasion.application.society

import io.github.mindzard.mythicinvasion.application.ai.StrategyActionParser
import io.github.mindzard.mythicinvasion.application.ai.StrategyExecutionState
import io.github.mindzard.mythicinvasion.concurrency.CoroutineEngine
import io.github.mindzard.mythicinvasion.domain.ai.StrategyAction
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
import java.util.UUID

class PillagerFactionCoordinator(
    private val plugin: JavaPlugin,
    private val coroutineEngine: CoroutineEngine,
    private val stateStore: SocietyStateStore,
    private val socialStore: SettlementSocialStore,
    private val strategyStore: PillagerStrategyStore,
    private val targetingEngine: PillagerSettlementTargetingEngine,
    private val strategyExecutionState: StrategyExecutionState,
    private val strategyActionParser: StrategyActionParser,
    private val updateIntervalMillis: () -> Long,
    private val assignmentRadius: Double
) {

    companion object {
        private const val FACTION_ID = "pillagers"

        private const val TARGET_KEY = "target_settlement"
        private const val MODE_KEY = "strategy_mode"
        private const val PHASE_KEY = "strategy_phase"
        private const val ROLE_KEY = "strategy_role"
        private const val ASSIGNED_AT_KEY = "strategy_assigned_at"

        private const val SCOUT_MODE = "SCOUT"
        private const val PRESSURE_MODE = "PRESSURE"
        private const val RESERVE_MODE = "RESERVE"

        private const val SCOUT_PHASE = "SCOUTING"
        private const val PRESSURE_PHASE = "PRESSURE"
        private const val REGROUP_PHASE = "REGROUPING"

        private const val TARGET_STICKINESS = 0.10
        private const val RETARGET_INTERVAL_MILLIS = 30_000L

        private const val MAX_ASSIGNED_UNITS = 16
        private const val BASE_SCOUT_UNITS = 2

        private const val APPROACH_DISTANCE = 24.0
        private const val SECURITY_RING_DISTANCE = 26.0
        private const val SCOUT_RING_DISTANCE = 40.0

        private const val SCOUT_SPEED = 0.70
        private const val PRESSURE_SPEED = 0.78
        private const val RESERVE_SPEED = 0.60

        private const val PLAYER_SEARCH_RADIUS = 36.0
        private const val TARGET_MEMORY_BONUS = 0.25
    }

    private var job: Job? = null

    private lateinit var targetSettlementKey: NamespacedKey
    private lateinit var strategyModeKey: NamespacedKey
    private lateinit var strategyPhaseKey: NamespacedKey
    private lateinit var roleKey: NamespacedKey
    private lateinit var assignedAtKey: NamespacedKey

    fun start() {
        if (job != null) {
            return
        }

        targetSettlementKey =
            NamespacedKey(plugin, TARGET_KEY)

        strategyModeKey =
            NamespacedKey(plugin, MODE_KEY)

        strategyPhaseKey =
            NamespacedKey(plugin, PHASE_KEY)

        roleKey =
            NamespacedKey(plugin, ROLE_KEY)

        assignedAtKey =
            NamespacedKey(plugin, ASSIGNED_AT_KEY)

        job =
            coroutineEngine.scope.launch {
                plugin.logger.info(
                    "Pillager faction intelligence started."
                )

                while (isActive) {
                    try {
                        plugin.server.scheduler
                            .callSyncMethod(plugin) {
                                runCycle()
                            }
                            .get()
                    } catch (exception: Exception) {
                        plugin.logger.warning(
                            "Pillager strategy cycle failed: " +
                                "${exception.javaClass.simpleName}: " +
                                "${exception.message}"
                        )
                    }

                    delay(updateIntervalMillis())
                }
            }
    }

    fun stop() {
        job?.cancel()
        job = null

        if (::targetSettlementKey.isInitialized) {
            clearEntityStrategyTags()
        }

        strategyStore.clear()
    }

    private fun runCycle() {
        val settlements =
            stateStore.current()
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
                    !it.isDead &&
                        it.isValid
                }

        updateFactionState(
            pillagerCount = pillagers.size
        )

        if (pillagers.isEmpty()) {
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

        if (settlements.isEmpty()) {
            runLocalFactionSecurity(
                pillagers
            )

            strategyStore.update(
                PillagerStrategyState(
                    targetReason =
                        "No settlement target; local faction security active.",
                    assignedPillagerCount =
                        pillagers.size,
                    scoutingPillagerCount =
                        countRole(
                            pillagers,
                            "SCOUT"
                        ),
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
                    val nearby =
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
                            nearby
                    )
                }
                .sortedByDescending {
                    it.score
                }

        val selectedScore =
            chooseStableTarget(
                scoredSettlements
            ) ?: return

        val selectedSettlement =
            settlements.firstOrNull {
                it.settlementId ==
                    selectedScore.settlementId
            } ?: return

        val previous =
            strategyStore.current()

        val targetChanged =
            previous.selectedSettlementId !=
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
                    assignment.assignedUnits,
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
                    " | scouts=" +
                    assignment.scoutUnits +
                    " | assigned=" +
                    assignment.assignedUnits
            )
        }
    }

    private fun chooseStableTarget(
        scored:
            List<PillagerSettlementTargetScore>
    ): PillagerSettlementTargetScore? {
        val best =
            scored.firstOrNull()
                ?: return null

        val currentId =
            strategyStore.current()
                .selectedSettlementId
                ?: return best

        val current =
            scored.firstOrNull {
                it.settlementId ==
                    currentId
            } ?: return best

        val age =
            System.currentTimeMillis() -
                strategyStore.current()
                    .updatedAtMillis

        if (age >= RETARGET_INTERVAL_MILLIS) {
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
        pillagers: List<Pillager>,
        settlement: SettlementState,
        targetScore: Double
    ): AssignmentResult {
        val center =
            settlementCenter(
                settlement
            )

        val selected =
            pillagers
                .filter {
                    it.world.name ==
                        settlement.worldName
                }
                .map { pillager ->
                    pillager to
                        distanceSquared(
                            pillager.location,
                            center
                        )
                }
                .filter {
                    it.second <=
                        assignmentRadius *
                        assignmentRadius
                }
                .sortedBy {
                    it.second
                }
                .take(
                    MAX_ASSIGNED_UNITS
                )

        if (selected.isEmpty()) {
            clearEntityStrategyTagsForOthers(
                emptySet(),
                pillagers
            )
            return AssignmentResult()
        }

        val aiActions =
            activeStrategyActions()

        val scoutBonus =
            if (
                StrategyAction.SCOUT_SETTLEMENTS in
                    aiActions
            ) {
                2
            } else {
                0
            }

        val aggressive =
            StrategyAction.INCREASE_HOSTILE_PRESSURE in
                aiActions

        val focusPlayers =
            StrategyAction.FOCUS_HIGH_PRESSURE_PLAYERS in
                aiActions

        val scoutCount =
            min(
                (selected.size / 3)
                    .coerceAtLeast(1)
                    .plus(scoutBonus),
                4
            )

        val selectedIds =
            selected.map {
                it.first.uniqueId
            }.toSet()

        var scouts = 0

        selected.forEachIndexed {
            index,
            pair ->

            val pillager =
                pair.first

            val role =
                when {
                    index < scoutCount ->
                        "SCOUT"

                    index % 4 == 1 ->
                        "FLANK_LEFT"

                    index % 4 == 2 ->
                        "FLANK_RIGHT"

                    else ->
                        "RESERVE"
                }

            val phase =
                when {
                    role == "SCOUT" ->
                        SCOUT_PHASE

                    targetScore >= 0.60 ||
                        aggressive ->
                        PRESSURE_PHASE

                    else ->
                        REGROUP_PHASE
                }

            val mode =
                when {
                    role == "RESERVE" ->
                        RESERVE_MODE

                    role.startsWith("FLANK") ||
                        role == "SCOUT" ->
                        PRESSURE_MODE

                    else ->
                        PRESSURE_MODE
                }

            writeStrategyMemory(
                pillager =
                    pillager,
                settlementId =
                    settlement.settlementId,
                mode =
                    mode,
                phase =
                    phase,
                role =
                    role
            )

            when (phase) {
                SCOUT_PHASE -> {
                    scouts++

                    executeScoutBehaviour(
                        pillager,
                        settlement
                    )
                }

                PRESSURE_PHASE -> {
                    executeRoleBehaviour(
                        pillager =
                            pillager,
                        settlement =
                            settlement,
                        role =
                            role,
                        focusPlayers =
                            focusPlayers
                    )
                }

                else -> {
                    executeReserveBehaviour(
                        pillager,
                        settlement,
                        role
                    )
                }
            }
        }

        clearEntityStrategyTagsForOthers(
            selectedIds,
            pillagers
        )

        return AssignmentResult(
            assignedUnits =
                selected.size,
            scoutUnits =
                scouts
        )
    }

    private fun executeScoutBehaviour(
        pillager: Pillager,
        settlement: SettlementState
    ) {
        val location =
            calculateRingLocation(
                center =
                    settlementCenter(
                        settlement
                    ),
                index =
                    stableIndex(
                        pillager.uniqueId
                    ),
                count =
                    4,
                radius =
                    min(
                        SCOUT_RING_DISTANCE,
                        settlement.radius * 1.25
                    )
            )

        if (
            distanceSquared(
                pillager.location,
                location
            ) > 25.0
        ) {
            pillager.pathfinder.moveTo(
                location,
                SCOUT_SPEED
            )
        }

        val threat =
            findHighValueThreat(
                pillager,
                settlement
            )

        if (threat != null) {
            pillager.lookAt(threat)
        }
    }

    private fun executeRoleBehaviour(
        pillager: Pillager,
        settlement: SettlementState,
        role: String,
        focusPlayers: Boolean
    ) {
        val center =
            settlementCenter(
                settlement
            )

        val target =
            if (focusPlayers) {
                findHighestThreatPlayer(
                    pillager,
                    settlement
                )
            } else {
                findNearbySettlementTarget(
                    pillager,
                    settlement
                )
            }

        if (target != null) {
            val roleIndex =
                when (role) {
                    "FLANK_LEFT" -> 1
                    "FLANK_RIGHT" -> 3
                    else -> 2
                }

            val flank =
                calculateRingLocation(
                    center =
                        target.location,
                    index =
                        roleIndex,
                    count =
                        4,
                    radius =
                        10.0
                )

            if (
                distanceSquared(
                    pillager.location,
                    flank
                ) > 20.0
            ) {
                pillager.pathfinder.moveTo(
                    flank,
                    PRESSURE_SPEED
                )
            }

            /*
             * Only the close pressure unit commits its vanilla
             * combat target. Other units hold their assigned
             * positions, which prevents the old "everyone swarms
             * the same block" look.
             */
            if (
                distanceSquared(
                    pillager.location,
                    target.location
                ) <= 18.0
            ) {
                pillager.target =
                    target
            } else {
                pillager.target = null
            }

            pillager.lookAt(target)
            return
        }

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

    private fun executeReserveBehaviour(
        pillager: Pillager,
        settlement: SettlementState,
        role: String
    ) {
        val center =
            settlementCenter(
                settlement
            )

        val index =
            stableIndex(
                pillager.uniqueId
            ) +
                if (
                    role == "FLANK_RIGHT"
                ) {
                    2
                } else {
                    0
                }

        val security =
            calculateRingLocation(
                center =
                    center,
                index =
                    index,
                count =
                    6,
                radius =
                    min(
                        SECURITY_RING_DISTANCE,
                        settlement.radius.toDouble()
                    )
            )

        if (
            distanceSquared(
                pillager.location,
                security
            ) > 16.0
        ) {
            pillager.pathfinder.moveTo(
                security,
                RESERVE_SPEED
            )
        }

        val threat =
            findHighValueThreat(
                pillager,
                settlement
            )

        if (threat != null) {
            pillager.lookAt(threat)
        }
    }

    private fun runLocalFactionSecurity(
        pillagers: List<Pillager>
    ) {
        val grouped =
            pillagers.groupBy {
                it.world.uid
            }

        for ((_, units) in grouped) {
            if (units.isEmpty()) {
                continue
            }

            val center =
                averageLocation(
                    units
                )

            units.take(
                MAX_ASSIGNED_UNITS
            ).forEachIndexed {
                index,
                pillager ->

                val role =
                    if (index < 2) {
                        "SCOUT"
                    } else if (
                        index % 2 == 0
                    ) {
                        "RESERVE"
                    } else {
                        "PRESSURE"
                    }

                writeStrategyMemory(
                    pillager =
                        pillager,
                    settlementId =
                        "local-faction-security",
                    mode =
                        if (
                            role == "SCOUT"
                        ) {
                            SCOUT_MODE
                        } else {
                            PRESSURE_MODE
                        },
                    phase =
                        if (
                            role == "SCOUT"
                        ) {
                            SCOUT_PHASE
                        } else {
                            PRESSURE_PHASE
                        },
                    role =
                        role
                )

                if (role == "SCOUT") {
                    val patrol =
                        calculateRingLocation(
                            center =
                                center,
                            index =
                                index,
                            count =
                                4,
                            radius =
                                18.0
                        )

                    if (
                        distanceSquared(
                            pillager.location,
                            patrol
                        ) > 16.0
                    ) {
                        pillager.pathfinder.moveTo(
                            patrol,
                            SCOUT_SPEED
                        )
                    }
                } else {
                    val threat =
                        findNearbyPlayer(
                            pillager
                        )

                    if (threat != null) {
                        pillager.lookAt(threat)

                        if (
                            distanceSquared(
                                pillager.location,
                                threat.location
                            ) <= 18.0
                        ) {
                            pillager.target = threat
                        } else {
                            pillager.target = null
                        }
                    }
                }
            }
        }
    }

    private fun activeStrategyActions(): Set<StrategyAction> {
        val decision =
            strategyExecutionState.current()
                ?: return emptySet()

        return decision.suggestedActions
            .map(
                strategyActionParser::parse
            )
            .toSet()
    }

    private fun findHighestThreatPlayer(
        pillager: Pillager,
        settlement: SettlementState
    ): Player? {
        val players =
            pillager.world.players
                .filter {
                    it.isOnline &&
                        !it.isDead
                }
                .filter {
                    distanceSquared(
                        it.location,
                        settlementCenter(
                            settlement
                        )
                    ) <=
                        (
                            settlement.radius +
                                24
                            ) *
                        (
                            settlement.radius +
                                24
                            )
                }

        val social =
            socialStore
                .snapshot()
                .firstOrNull {
                    it.settlementId ==
                        settlement.settlementId
                }

        if (social == null) {
            return players.minByOrNull {
                distanceSquared(
                    pillager.location,
                    it.location
                )
            }
        }

        val threatMap =
            social.hostilePlayers

        return players.maxByOrNull { player ->
            (
                threatMap[player.uniqueId]
                    ?: 0.0
            ) +
                if (
                    player == pillager.target
                ) {
                    TARGET_MEMORY_BONUS
                } else {
                    0.0
                }
        }
    }

    private fun findHighValueThreat(
        pillager: Pillager,
        settlement: SettlementState
    ): LivingEntity? {
        return findHighestThreatPlayer(
            pillager,
            settlement
        ) ?: findNearbySettlementTarget(
            pillager,
            settlement
        )
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
            pillager.world.getNearbyEntities(
                pillager.location,
                PLAYER_SEARCH_RADIUS,
                12.0,
                PLAYER_SEARCH_RADIUS
            )

        return nearby
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
                            16
                        ) *
                        (
                            settlement.radius +
                                16
                            )
            }
            .minByOrNull {
                distanceSquared(
                    it.location,
                    pillager.location
                )
            }
            ?: nearby
                .asSequence()
                .filterIsInstance<Villager>()
                .filter {
                    !it.isDead
                }
                .minByOrNull {
                    distanceSquared(
                        it.location,
                        pillager.location
                    )
                }
    }

    private fun findNearbyPlayer(
        pillager: Pillager
    ): Player? {
        return pillager.world
            .getNearbyPlayers(
                pillager.location,
                PLAYER_SEARCH_RADIUS
            )
            .filter {
                it.isOnline &&
                    !it.isDead
            }
            .minByOrNull {
                distanceSquared(
                    it.location,
                    pillager.location
                )
            }
    }

    private fun calculateRingLocation(
        center: Location,
        index: Int,
        count: Int,
        radius: Double
    ): Location {
        val safeCount =
            count.coerceAtLeast(1)

        val angle =
            (
                2.0 *
                    PI /
                    safeCount.toDouble()
                ) *
                (
                    index %
                        safeCount
                )

        return Location(
            center.world,
            center.x +
                cos(angle) *
                radius,
            center.y,
            center.z +
                sin(angle) *
                radius
        )
    }

    private fun writeStrategyMemory(
        pillager: Pillager,
        settlementId: String,
        mode: String,
        phase: String,
        role: String
    ) {
        pillager.persistentDataContainer.set(
            targetSettlementKey,
            PersistentDataType.STRING,
            settlementId
        )

        pillager.persistentDataContainer.set(
            strategyModeKey,
            PersistentDataType.STRING,
            mode
        )

        pillager.persistentDataContainer.set(
            strategyPhaseKey,
            PersistentDataType.STRING,
            phase
        )

        pillager.persistentDataContainer.set(
            roleKey,
            PersistentDataType.STRING,
            role
        )

        pillager.persistentDataContainer.set(
            assignedAtKey,
            PersistentDataType.LONG,
            System.currentTimeMillis()
        )
    }

    private fun clearEntityStrategyTags() {
        for (world in plugin.server.worlds) {
            world.getEntitiesByClass(
                Pillager::class.java
            ).forEach {
                clearStrategyMemory(it)
            }
        }
    }

    private fun clearEntityStrategyTagsForOthers(
        selectedIds: Set<UUID>,
        allPillagers: List<Pillager>
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
            !::targetSettlementKey.isInitialized
        ) {
            return
        }

        pillager.persistentDataContainer.remove(
            targetSettlementKey
        )

        pillager.persistentDataContainer.remove(
            strategyModeKey
        )

        pillager.persistentDataContainer.remove(
            strategyPhaseKey
        )

        pillager.persistentDataContainer.remove(
            roleKey
        )

        pillager.persistentDataContainer.remove(
            assignedAtKey
        )
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
                pillagerCount / 25.0,
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

        if (world == null) {
            return plugin.server
                .worlds
                .first()
                .spawnLocation
        }

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

    private fun averageLocation(
        units: List<Pillager>
    ): Location {
        val first =
            units.first()

        val x =
            units
                .map {
                    it.location.x
                }
                .average()

        val y =
            units
                .map {
                    it.location.y
                }
                .average()

        val z =
            units
                .map {
                    it.location.z
                }
                .average()

        return Location(
            first.world,
            x,
            y,
            z
        )
    }

    private fun countRole(
        pillagers: List<Pillager>,
        role: String
    ): Int {
        if (!::roleKey.isInitialized) {
            return 0
        }

        return pillagers.count {
            it.persistentDataContainer.get(
                roleKey,
                PersistentDataType.STRING
            ) == role
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
        id: UUID
    ): Int {
        return (
            id.mostSignificantBits xor
                id.leastSignificantBits
            )
            .toInt()
            .absoluteValue
    }

    private fun debugEnabled(): Boolean =
        plugin.config.getBoolean(
            "plugin.debug",
            false
        )

    private data class AssignmentResult(
        val assignedUnits: Int = 0,
        val scoutUnits: Int = 0
    )
}
