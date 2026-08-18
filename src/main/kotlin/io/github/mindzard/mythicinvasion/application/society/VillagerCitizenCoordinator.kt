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
                    delay(updateIntervalMillis())
                }
            }
    }

    fun stop() {
        job?.cancel()
        job = null
        actionCooldowns.clear()
    }

    private fun processVillagers() {
        val settlements =
            stateStore.current()
                .settlements
                .values
                .toList()

        if (settlements.isEmpty()) {
            return
        }

        for (world in plugin.server.worlds) {
            for (
                villager in
                world.getEntitiesByClass(
                    Villager::class.java
                )
            ) {
                if (villager.isDead) {
                    continue
                }

                val settlement =
                    findSettlement(
                        villager.location,
                        settlements
                    ) ?: continue

                processVillager(
                    villager,
                    settlement
                )
            }
        }
    }

    private fun processVillager(
        villager: Villager,
        settlement:
            io.github.mindzard.mythicinvasion
                .domain.society.SettlementState
    ) {
        val now =
            System.currentTimeMillis()

        val nextAllowed =
            actionCooldowns[villager.uniqueId]

        if (
            nextAllowed != null &&
            now < nextAllowed
        ) {
            return
        }

        val danger =
            findNearbyDanger(
                villager
            )

        if (danger != null) {
            handleDanger(
                villager,
                danger,
                settlement
            )

            setCooldown(
                villager,
                now
            )

            return
        }

        /*
         * Normal life deliberately stays mostly under vanilla
         * Villager AI. We only inject relationship-aware signals
         * when a relevant player is close enough.
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

        if (nearbyPlayers.isEmpty()) {
            return
        }

        val relationships =
            nearbyPlayers.mapNotNull { player ->
                relationshipStore.get(
                    villager.uniqueId,
                    player.uniqueId
                )?.let {
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
                villager,
                hostile.first,
                settlement,
                hostile.second
            )

            setCooldown(
                villager,
                now
            )

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
                villager,
                friendly.first
            )

            setCooldown(
                villager,
                now
            )
        }
    }

    private fun handleDanger(
        villager: Villager,
        danger: Monster,
        settlement:
            io.github.mindzard.mythicinvasion
                .domain.society.SettlementState
    ) {
        val retreat =
            calculateRetreat(
                villager.location,
                danger.location,
                settlement,
                10.0
            )

        if (retreat != null) {
            villager.pathfinder.moveTo(
                retreat,
                0.90
            )
        }

        /*
         * One memory is enough for the native brain to retain
         * awareness. We do not repeatedly force the villager to
         * stare at the threat.
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
                    "detected ${danger.type}"
            )
        }
    }

    private fun handleHostilePlayer(
        villager: Villager,
        player: Player,
        settlement:
            io.github.mindzard.mythicinvasion
                .domain.society.SettlementState,
        relationship: PlayerVillagerRelationship
    ) {
        villager.setMemory(
            MemoryKey.ANGRY_AT,
            player.uniqueId
        )

        val retreat =
            calculateRetreat(
                villager.location,
                player.location,
                settlement,
                8.0
            )

        if (retreat != null) {
            villager.pathfinder.moveTo(
                retreat,
                0.85
            )
        }

        if (debugEnabled()) {
            plugin.logger.info(
                "Villager ${villager.uniqueId} " +
                    "reacted to hostile player " +
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
         * Set a real native memory signal, but do not force
         * every villager to walk into the player. That was one
         * of the causes of unnatural crowding.
         */
        villager.setMemory(
            MemoryKey.LIKED_PLAYER,
            player.uniqueId
        )
    }

    private fun findNearbyDanger(
        villager: Villager
    ): Monster? {
        return villager.world
            .getNearbyEntities(
                villager.location,
                10.0,
                6.0,
                10.0
            )
            .asSequence()
            .filterIsInstance<Monster>()
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

    private fun calculateRetreat(
        villagerLocation: Location,
        threatLocation: Location,
        settlement:
            io.github.mindzard.mythicinvasion
                .domain.society.SettlementState,
        retreatDistance: Double
    ): Location? {
        var dx =
            villagerLocation.x -
                threatLocation.x

        var dz =
            villagerLocation.z -
                threatLocation.z

        val length =
            sqrt(
                dx * dx +
                    dz * dz
            )

        if (length <= 0.001) {
            dx = 1.0
            dz = 0.0
        } else {
            dx /= length
            dz /= length
        }

        val raw =
            Location(
                villagerLocation.world,
                villagerLocation.x +
                    dx *
                    retreatDistance,
                villagerLocation.y,
                villagerLocation.z +
                    dz *
                    retreatDistance
            )

        return clampInsideSettlement(
            raw,
            settlement
        )
    }

    private fun clampInsideSettlement(
        location: Location,
        settlement:
            io.github.mindzard.mythicinvasion
                .domain.society.SettlementState
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

        if (distance <= 0.001) {
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
                dx * scale,
            location.y,
            centerZ +
                dz * scale
        )
    }

    private fun findSettlement(
        location: Location,
        settlements:
            Collection<
                io.github.mindzard.mythicinvasion
                    .domain.society.SettlementState
            >
    ):
        io.github.mindzard.mythicinvasion
            .domain.society.SettlementState? {
        val world =
            location.world ?: return null

        return settlements
            .asSequence()
            .filter {
                it.worldName == world.name
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

                settlement to
                    (
                        dx * dx +
                            dy * dy +
                            dz * dz
                        )
            }
            .filter { (settlement, distance) ->
                val radius =
                    settlement.radius.toDouble()

                distance <=
                    radius * radius
            }
            .minByOrNull {
                it.second
            }
            ?.first
    }

    private fun setCooldown(
        villager: Villager,
        now: Long
    ) {
        actionCooldowns[
            villager.uniqueId
        ] =
            now +
                actionCooldownMillis
    }

    private fun cleanupCooldowns() {
        val now =
            System.currentTimeMillis()

        actionCooldowns.entries.removeIf {
            it.value <= now
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

    private fun debugEnabled(): Boolean =
        plugin.config.getBoolean(
            "plugin.debug",
            false
        )
}
