package io.github.mindzard.mythicinvasion.application.society

import io.github.mindzard.mythicinvasion.concurrency.CoroutineEngine
import io.github.mindzard.mythicinvasion.domain.society.PlayerVillagerRelationship
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.bukkit.Location
import org.bukkit.entity.Monster
import org.bukkit.entity.Pillager
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import org.bukkit.entity.memory.MemoryKey
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

class VillagerCitizenCoordinator(
    private val plugin: JavaPlugin,
    private val coroutineEngine: CoroutineEngine,
    private val stateStore: SocietyStateStore,
    private val relationshipStore: VillagerRelationshipStore,
    private val updateIntervalMillis: () -> Long,
    private val interactionRadius: Double,
    private val hostileRadius: Double,
    private val friendlyLookRadius: Double,
    private val hostileThreatThreshold: Double,
    private val friendlyTrustThreshold: Double,
    private val actionCooldownMillis: Long
) {

    private var job: Job? = null

    private val actionCooldowns =
        ConcurrentHashMap<UUID, Long>()

    fun start() {

        if (job != null) {
            return
        }

        job =
            coroutineEngine.scope.launch {

                plugin.logger.info(
                    "Villager citizen behaviour coordinator started."
                )

                while (isActive) {

                    try {

                        plugin.server.scheduler
                            .callSyncMethod(plugin) {
                                processVillagers()
                            }
                            .get()

                    } catch (exception: Exception) {

                        plugin.logger.warning(
                            "Villager citizen cycle failed: " +
                                "${exception.javaClass.simpleName}: " +
                                "${exception.message}"
                        )
                    }

                    cleanupCooldowns()

                    delay(
                        updateIntervalMillis()
                    )
                }
            }
    }

    fun stop() {

        job?.cancel()
        job = null

        actionCooldowns.clear()

        plugin.logger.info(
            "Villager citizen behaviour coordinator stopped."
        )
    }

    private fun processVillagers() {

        val settlements =
            stateStore
                .current()
                .settlements
                .values
                .toList()

        if (settlements.isEmpty()) {
            return
        }

        for (world in plugin.server.worlds) {

            val villagers =
                world.getEntitiesByClass(
                    Villager::class.java
                )

            for (villager in villagers) {

                if (villager.isDead) {
                    continue
                }

                val settlement =
                    findSettlement(
                        villager.location,
                        settlements
                    )
                        ?: continue

                processVillager(
                    villager = villager,
                    settlement = settlement
                )
            }
        }
    }

    private fun processVillager(
        villager: Villager,
        settlement: io.github.mindzard.mythicinvasion.domain.society.SettlementState
    ) {

        val now =
            System.currentTimeMillis()

        val nextAllowed =
            actionCooldowns[
                villager.uniqueId
            ]

        if (
            nextAllowed != null &&
            now < nextAllowed
        ) {
            return
        }

        /*
         * Highest priority:
         * nearby immediate danger.
         */
        val danger =
            findNearbyDanger(
                villager
            )

        if (danger != null) {

            handleDanger(
                villager = villager,
                danger = danger,
                settlement = settlement
            )

            actionCooldowns[
                villager.uniqueId
            ] =
                now +
                    actionCooldownMillis

            return
        }

        /*
         * Second priority:
         * relationship-driven player behaviour.
         */
        val nearbyPlayers =
            villager.location
                .getNearbyPlayers(
                    interactionRadius
                )
                .filter {
                    it.isOnline &&
                        !it.isDead
                }

        val relationships =
            nearbyPlayers
                .mapNotNull { player ->

                    relationshipStore.get(
                        villagerId =
                            villager.uniqueId,
                        playerId =
                            player.uniqueId
                    )
                        ?.let {
                            player to it
                        }
                }

        val hostile =
            relationships
                .asSequence()
                .filter { (_, relationship) ->

                    relationship.threat >=
                        hostileThreatThreshold
                }
                .filter { (player, _) ->

                    distanceSquared(
                        villager.location,
                        player.location
                    ) <=
                        hostileRadius *
                        hostileRadius
                }
                .maxByOrNull { (_, relationship) ->

                    relationship.threat
                }

        if (hostile != null) {

            handleHostilePlayer(
                villager = villager,
                player = hostile.first,
                settlement = settlement,
                relationship = hostile.second
            )

            actionCooldowns[
                villager.uniqueId
            ] =
                now +
                    actionCooldownMillis

            return
        }

        val friendly =
            relationships
                .asSequence()
                .filter { (_, relationship) ->

                    relationship.trust >=
                        friendlyTrustThreshold
                }
                .filter { (player, _) ->

                    distanceSquared(
                        villager.location,
                        player.location
                    ) <=
                        friendlyLookRadius *
                        friendlyLookRadius
                }
                .maxByOrNull { (_, relationship) ->

                    relationship.trust
                }

        if (friendly != null) {

            handleFriendlyPlayer(
                villager = villager,
                player = friendly.first
            )

            actionCooldowns[
                villager.uniqueId
            ] =
                now +
                    actionCooldownMillis

            return
        }

        /*
         * Third priority:
         * villagers naturally acknowledge nearby citizens.
         *
         * We intentionally do not spam movement or force a new
         * path every tick. Native villager AI remains responsible
         * for most movement.
         */
        val nearbyVillager =
            findNearestVillager(
                villager
            )

        if (nearbyVillager != null) {

            handleSocialAwareness(
                villager = villager,
                otherVillager = nearbyVillager
            )

            actionCooldowns[
                villager.uniqueId
            ] =
                now +
                    actionCooldownMillis
        }
    }

    private fun findNearbyDanger(
        villager: Villager
    ): Monster? {

        val nearbyEntities =
            villager.world
                .getNearbyEntities(
                    villager.location,
                    10.0,
                    6.0,
                    10.0
                )

        return nearbyEntities
            .asSequence()
            .filterIsInstance<Monster>()
            .filter {
                !it.isDead
            }
            .filter {
                it !is Villager
            }
            .minByOrNull {
                distanceSquared(
                    villager.location,
                    it.location
                )
            }
    }

    private fun handleDanger(
        villager: Villager,
        danger: Monster,
        settlement:
            io.github.mindzard.mythicinvasion.domain.society.SettlementState
    ) {

        /*
         * Danger response is defensive.
         *
         * We do not force villagers to attack.
         * We move them toward a safer point inside their settlement.
         */
        val retreat =
            calculateSafeRetreatLocation(
                villager = villager,
                danger = danger,
                settlement = settlement
            )

        if (retreat != null) {

            villager.pathfinder.moveTo(
                retreat,
                0.90
            )
        }

        villager.lookAt(
            danger
        )

        /*
         * ANGRY_AT is reserved for actual threat memory.
         * The native villager brain can use this signal.
         */
        if (danger is Pillager) {

            villager.setMemory(
                MemoryKey.ANGRY_AT,
                danger.uniqueId
            )
        }

        if (debugEnabled()) {

            plugin.logger.info(
                "Villager ${villager.uniqueId} " +
                    "detected danger ${danger.type}."
            )
        }
    }

    private fun handleHostilePlayer(
        villager: Villager,
        player: Player,
        settlement:
            io.github.mindzard.mythicinvasion.domain.society.SettlementState,
        relationship: PlayerVillagerRelationship
    ) {

        /*
         * Villagers should feel cautious, not magically aggressive.
         */
        villager.setMemory(
            MemoryKey.ANGRY_AT,
            player.uniqueId
        )

        val retreat =
            calculatePlayerRetreatLocation(
                villager = villager,
                player = player,
                settlement = settlement
            )

        if (retreat != null) {

            villager.pathfinder.moveTo(
                retreat,
                0.85
            )
        }

        villager.lookAt(
            player
        )

        if (debugEnabled()) {

            plugin.logger.info(
                "Villager ${villager.uniqueId} " +
                    "reacted defensively to " +
                    "${player.name} " +
                    "(threat=" +
                    "%.2f".format(
                        relationship.threat
                    ) +
                    ")"
            )
        }
    }

    private fun handleFriendlyPlayer(
        villager: Villager,
        player: Player
    ) {

        /*
         * Native villager memory keeps the positive signal alive.
         */
        villager.setMemory(
            MemoryKey.LIKED_PLAYER,
            player.uniqueId
        )

        villager.lookAt(
            player
        )

        /*
         * Small, non-spammy social acknowledgement.
         * Native pathfinding remains in charge.
         */
        if (
            distanceSquared(
                villager.location,
                player.location
            ) > 16.0
        ) {

            villager.pathfinder.moveTo(
                player.location,
                0.55
            )
        }

        if (debugEnabled()) {

            plugin.logger.info(
                "Villager ${villager.uniqueId} " +
                    "recognized trusted player " +
                    player.name
            )
        }
    }

    private fun handleSocialAwareness(
        villager: Villager,
        otherVillager: Villager
    ) {

        /*
         * Villagers occasionally acknowledge each other
         * instead of every villager permanently following
         * every other villager.
         */
        villager.lookAt(
            otherVillager
        )

        if (
            villager.profession ==
                Villager.Profession.FARMER
        ) {

            villager.memory(
                MemoryKey.JOB_SITE
            )
        }
    }

    private fun calculatePlayerRetreatLocation(
        villager: Villager,
        player: Player,
        settlement:
            io.github.mindzard.mythicinvasion.domain.society.SettlementState
    ): Location? {

        val villagerLocation =
            villager.location

        val playerLocation =
            player.location

        var dx =
            villagerLocation.x -
                playerLocation.x

        var dz =
            villagerLocation.z -
                playerLocation.z

        val distance =
            sqrt(
                dx * dx +
                    dz * dz
            )

        if (distance <= 0.001) {

            dx = 1.0
            dz = 0.0

        } else {

            dx /=
                distance

            dz /=
                distance
        }

        val retreatDistance =
            8.0

        return clampInsideSettlement(
            settlement =
                settlement,
            location =
                Location(
                    villager.world,
                    villagerLocation.x +
                        dx *
                        retreatDistance,
                    villagerLocation.y,
                    villagerLocation.z +
                        dz *
                        retreatDistance
                )
        )
    }

    private fun calculateSafeRetreatLocation(
        villager: Villager,
        danger: Monster,
        settlement:
            io.github.mindzard.mythicinvasion.domain.society.SettlementState
    ): Location? {

        val villagerLocation =
            villager.location

        val dangerLocation =
            danger.location

        var dx =
            villagerLocation.x -
                dangerLocation.x

        var dz =
            villagerLocation.z -
                dangerLocation.z

        val distance =
            sqrt(
                dx * dx +
                    dz * dz
            )

        if (distance <= 0.001) {

            dx = 1.0
            dz = 0.0

        } else {

            dx /=
                distance

            dz /=
                distance
        }

        val retreatDistance =
            10.0

        return clampInsideSettlement(
            settlement =
                settlement,
            location =
                Location(
                    villager.world,
                    villagerLocation.x +
                        dx *
                        retreatDistance,
                    villagerLocation.y,
                    villagerLocation.z +
                        dz *
                        retreatDistance
                )
        )
    }

    private fun clampInsideSettlement(
        settlement:
            io.github.mindzard.mythicinvasion.domain.society.SettlementState,
        location: Location
    ): Location? {

        val centerX =
            settlement.centerX.toDouble()

        val centerZ =
            settlement.centerZ.toDouble()

        val allowedRadius =
            (
                settlement.radius -
                    3
                )
                .coerceAtLeast(
                    4
                )
                .toDouble()

        val dx =
            location.x -
                centerX

        val dz =
            location.z -
                centerZ

        val distance =
            sqrt(
                dx * dx +
                    dz * dz
            )

        if (
            distance <=
                allowedRadius
        ) {
            return location
        }

        if (
            distance <=
                0.001
        ) {
            return Location(
                location.world,
                centerX,
                location.y,
                centerZ
            )
        }

        val scale =
            allowedRadius /
                distance

        return Location(
            location.world,
            centerX +
                dx *
                scale,
            location.y,
            centerZ +
                dz *
                scale
        )
    }

    private fun findNearestVillager(
        villager: Villager
    ): Villager? {

        return villager.world
            .getNearbyEntities(
                villager.location,
                8.0,
                4.0,
                8.0
            )
            .asSequence()
            .filterIsInstance<Villager>()
            .filter {
                it.uniqueId !=
                    villager.uniqueId
            }
            .filter {
                !it.isDead
            }
            .minByOrNull {
                distanceSquared(
                    villager.location,
                    it.location
                )
            }
    }

    private fun findSettlement(
        location: Location,
        settlements:
            Collection<
                io.github.mindzard.mythicinvasion.domain.society.SettlementState
            >
    ):
        io.github.mindzard.mythicinvasion.domain.society.SettlementState? {

        val world =
            location.world
                ?: return null

        return settlements
            .asSequence()
            .filter {
                it.worldName ==
                    world.name
            }
            .map { settlement ->

                val dx =
                    location.x -
                        settlement.centerX

                val dy =
                    location.y -
                        settlement.centerY

                val dz =
                    location.z -
                        settlement.centerZ

                val distanceSquared =
                    dx * dx +
                        dy * dy +
                        dz * dz

                settlement to
                    distanceSquared
            }
            .filter { (settlement, distanceSquared) ->

                val radius =
                    settlement.radius
                        .toDouble()

                distanceSquared <=
                    radius *
                    radius
            }
            .minByOrNull {
                it.second
            }
            ?.first
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

    private fun cleanupCooldowns() {

        val now =
            System.currentTimeMillis()

        actionCooldowns.entries.removeIf {
            it.value <=
                now
        }
    }

    private fun debugEnabled(): Boolean {

        return plugin.config.getBoolean(
            "plugin.debug",
            false
        )
    }

    private fun Villager.memory(
        key: MemoryKey<UUID>
    ) {
        getMemory(
            key
        )
    }
}
