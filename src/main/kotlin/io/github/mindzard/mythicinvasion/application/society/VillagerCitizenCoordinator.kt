package io.github.mindzard.mythicinvasion.application.society

import io.github.mindzard.mythicinvasion.concurrency.CoroutineEngine
import io.github.mindzard.mythicinvasion.domain.society.PlayerVillagerRelationship
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.bukkit.Location
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

                    } catch (
                        exception: Exception
                    ) {

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

        if (
            settlements.isEmpty()
        ) {
            return
        }

        for (world in plugin.server.worlds) {

            val villagers =
                world.getEntitiesByClass(
                    Villager::class.java
                )

            for (villager in villagers) {

                if (
                    villager.isDead
                ) {
                    continue
                }

                val settlement =
                    findSettlement(
                        villager.location,
                        settlements
                    )
                        ?: continue

                processVillager(
                    villager =
                        villager,
                    settlementId =
                        settlement.settlementId,
                    settlement =
                        settlement
                )
            }
        }
    }

    private fun processVillager(
        villager: Villager,
        settlementId: String,
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

        val nearbyPlayers =
            villager.location
                .getNearbyPlayers(
                    interactionRadius
                )
                .filter {
                    it.isOnline &&
                        !it.isDead
                }

        if (
            nearbyPlayers.isEmpty()
        ) {
            return
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

        if (
            relationships.isEmpty()
        ) {
            return
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

        if (
            hostile != null
        ) {

            handleHostilePlayer(
                villager =
                    villager,

                player =
                    hostile.first,

                settlement =
                    settlement,

                relationship =
                    hostile.second
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

        if (
            friendly != null
        ) {

            handleFriendlyPlayer(
                villager =
                    villager,

                player =
                    friendly.first
            )

            actionCooldowns[
                villager.uniqueId
            ] =
                now +
                    actionCooldownMillis
        }
    }

    private fun handleHostilePlayer(
        villager: Villager,
        player: Player,
        settlement: io.github.mindzard.mythicinvasion.domain.society.SettlementState,
        relationship: PlayerVillagerRelationship
    ) {

        /*
         * Do not attack the player.
         *
         * The first citizen behaviour is defensive:
         * villagers recognize danger and create distance.
         */

        villager.memory(
            MemoryKey.ANGRY_AT,
            player.uniqueId
        )

        val retreat =
            calculateRetreatLocation(
                villager =
                    villager,

                player =
                    player,

                settlement =
                    settlement
            )

        if (
            retreat != null
        ) {

            villager
                .pathfinder
                .moveTo(
                    retreat,
                    0.80
                )
        }

        villager.lookAt(
            player
        )

        if (
            plugin.config.getBoolean(
                "plugin.debug",
                false
            )
        ) {

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
         * LIKED_PLAYER is a real villager memory in Paper.
         * This lets the native villager brain retain the
         * relationship signal instead of replacing vanilla AI.
         */

        villager.setMemory(
            MemoryKey.LIKED_PLAYER,
            player.uniqueId
        )

        villager.lookAt(
            player
        )

        if (
            plugin.config.getBoolean(
                "plugin.debug",
                false
            )
        ) {

            plugin.logger.info(
                "Villager ${villager.uniqueId} " +
                    "recognized trusted player " +
                    player.name
            )
        }
    }

    private fun calculateRetreatLocation(
        villager: Villager,
        player: Player,
        settlement: io.github.mindzard.mythicinvasion.domain.society.SettlementState
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

        if (
            distance <= 0.001
        ) {

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

        var targetX =
            villagerLocation.x +
                dx *
                retreatDistance

        var targetZ =
            villagerLocation.z +
                dz *
                retreatDistance

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

        val centerDx =
            targetX -
                centerX

        val centerDz =
            targetZ -
                centerZ

        val centerDistance =
            sqrt(
                centerDx * centerDx +
                    centerDz * centerDz
            )

        if (
            centerDistance >
                allowedRadius
        ) {

            val scale =
                allowedRadius /
                    centerDistance

            targetX =
                centerX +
                    centerDx *
                    scale

            targetZ =
                centerZ +
                    centerDz *
                    scale
        }

        return Location(
            villager.world,
            targetX,
            villagerLocation.y,
            targetZ
        )
    }

    private fun findSettlement(
        location: Location,
        settlements:
            Collection<
                io.github.mindzard.mythicinvasion.domain.society.SettlementState
                >
    ): io.github.mindzard.mythicinvasion.domain.society.SettlementState? {

        return settlements
            .asSequence()
            .filter {
                it.worldName ==
                    location.world.name
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
                    settlement.radius.toDouble()

                distanceSquared <=
                    radius * radius
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
            it.value <= now
        }
    }

    private fun Villager.memory(
        key: MemoryKey<UUID>,
        value: UUID
    ) {
        setMemory(
            key,
            value
        )
    }
}
