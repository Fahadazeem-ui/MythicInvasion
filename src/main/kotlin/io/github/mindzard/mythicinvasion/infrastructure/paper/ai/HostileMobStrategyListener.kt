package io.github.mindzard.mythicinvasion.infrastructure.paper.ai

import io.github.mindzard.mythicinvasion.application.ai.AdaptiveTargetingEngine
import io.github.mindzard.mythicinvasion.application.ai.StrategyActionParser
import io.github.mindzard.mythicinvasion.application.ai.StrategyCooldownStore
import io.github.mindzard.mythicinvasion.application.ai.StrategyExecutionState
import io.github.mindzard.mythicinvasion.application.intelligence.BehaviourIntelligenceStore
import io.github.mindzard.mythicinvasion.application.society.SettlementSocialStore
import io.github.mindzard.mythicinvasion.application.society.SocietyStateStore
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.entity.Zombie
import org.bukkit.entity.ZombieVillager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

class HostileMobStrategyListener(
    private val plugin: JavaPlugin,
    private val intelligenceStore: BehaviourIntelligenceStore,
    private val societyStateStore: SocietyStateStore,
    private val settlementSocialStore: SettlementSocialStore,
    private val strategyExecutionState: StrategyExecutionState,
    private val actionParser: StrategyActionParser,
    private val cooldownStore: StrategyCooldownStore,
    private val adaptiveTargetingEngine: AdaptiveTargetingEngine,
    private val maximumRange: Double,
    private val minimumAdvantage: Double,
    private val cooldownMillis: Long
) : Listener {

    companion object {

        private const val MEMORY_HALF_LIFE_MILLIS =
            120_000L

        private const val MAX_MEMORY_SCORE =
            100.0

        private const val DAMAGE_MEMORY_GAIN =
            12.0

        private const val PLAYER_DAMAGE_MEMORY_GAIN =
            6.0

        private const val GROUP_RADIUS =
            12.0

        private const val MAX_GROUP_SIZE =
            8

        private const val MIN_TARGET_SCORE =
            0.10
    }

    private data class PlayerMemory(
        var score: Double,
        var lastUpdatedMillis: Long
    )

    private val playerMemory =
        ConcurrentHashMap<UUID, PlayerMemory>()

    private val targetCooldowns =
        ConcurrentHashMap<UUID, Long>()

    @EventHandler(
        priority = EventPriority.HIGHEST,
        ignoreCancelled = true
    )
    fun onZombieTarget(
        event: EntityTargetLivingEntityEvent
    ) {

        val zombie =
            event.entity as? Zombie
                ?: return

        if (
            zombie is ZombieVillager
        ) {
            return
        }

        if (
            !plugin.config.getBoolean(
                "adaptive-behaviour.enabled",
                true
            )
        ) {
            return
        }

        if (
            !plugin.config.getBoolean(
                "adaptive-behaviour.hostile-targeting.enabled",
                true
            )
        ) {
            return
        }

        val currentTarget =
            event.target as? Player

        val now =
            System.currentTimeMillis()

        val nextAllowed =
            targetCooldowns[
                zombie.uniqueId
            ]

        if (
            nextAllowed != null &&
            now < nextAllowed
        ) {
            return
        }

        val candidates =
            zombie.world.players
                .asSequence()
                .filter {
                    isValidPlayer(it)
                }
                .filter {
                    distanceSquared(
                        zombie,
                        it
                    ) <=
                        maximumRange *
                        maximumRange
                }
                .map { player ->

                    TargetScore(
                        player =
                            player,
                        score =
                            calculateTargetScore(
                                zombie =
                                    zombie,
                                player =
                                    player
                            )
                    )
                }
                .filter {
                    it.score >=
                        MIN_TARGET_SCORE
                }
                .sortedByDescending {
                    it.score
                }
                .toList()

        if (
            candidates.isEmpty()
        ) {
            return
        }

        val best =
            candidates.first()

        val currentScore =
            currentTarget?.let {
                candidates
                    .firstOrNull { candidate ->
                        candidate.player.uniqueId ==
                            it.uniqueId
                    }
                    ?.score
                    ?: calculateTargetScore(
                        zombie =
                            zombie,
                        player =
                            it
                    )
            }
                ?: 0.0

        val requiredAdvantage =
            max(
                0.03,
                minimumAdvantage
            )

        if (
            currentTarget != null &&
            best.player.uniqueId ==
                currentTarget.uniqueId
        ) {
            return
        }

        if (
            currentTarget != null &&
            best.score -
                currentScore <
                requiredAdvantage
        ) {
            return
        }

        event.target =
            best.player

        targetCooldowns[
            zombie.uniqueId
        ] =
            now +
                cooldownMillis

        if (
            debugEnabled()
        ) {

            plugin.logger.info(
                "Adaptive zombie target: " +
                    "${currentTarget?.name ?: "none"} -> " +
                    "${best.player.name} " +
                    "(score=" +
                    "%.2f".format(
                        best.score
                    ) +
                    ")"
            )
        }
    }

    @EventHandler(
        priority = EventPriority.MONITOR,
        ignoreCancelled = true
    )
    fun onZombieDamagesPlayer(
        event: EntityDamageByEntityEvent
    ) {

        val zombie =
            event.damager as? Zombie
                ?: return

        if (
            zombie is ZombieVillager
        ) {
            return
        }

        val player =
            event.entity as? Player
                ?: return

        if (
            !isValidPlayer(
                player
            )
        ) {
            return
        }

        addMemory(
            playerId =
                player.uniqueId,
            amount =
                PLAYER_DAMAGE_MEMORY_GAIN
        )
    }

    @EventHandler(
        priority = EventPriority.MONITOR,
        ignoreCancelled = true
    )
    fun onPlayerDamagesZombie(
        event: EntityDamageByEntityEvent
    ) {

        val player =
            event.damager as? Player
                ?: return

        val zombie =
            event.entity as? Zombie
                ?: return

        if (
            zombie is ZombieVillager
        ) {
            return
        }

        if (
            !isValidPlayer(
                player
            )
        ) {
            return
        }

        val bonus =
            if (
                event.damage >=
                    8.0
            ) {
                DAMAGE_MEMORY_GAIN * 1.5
            } else {
                DAMAGE_MEMORY_GAIN
            }

        addMemory(
            playerId =
                player.uniqueId,
            amount =
                bonus
        )
    }

    @EventHandler
    fun onPlayerQuit(
        event: PlayerQuitEvent
    ) {

        playerMemory.remove(
            event.player.uniqueId
        )
    }

    private fun calculateTargetScore(
        zombie: Zombie,
        player: Player
    ): Double {

        val distanceScore =
            calculateDistanceScore(
                zombie,
                player
            )

        val memoryScore =
            calculateMemoryScore(
                player.uniqueId
            )

        val groupScore =
            calculateGroupScore(
                zombie,
                player
            )

        val settlementScore =
            calculateSettlementScore(
                player
            )

        val daylightAdjustment =
            if (
                zombie.world.isDayTime
            ) {
                0.90
            } else {
                1.0
            }

        return (
            distanceScore * 0.40 +
                memoryScore * 0.30 +
                groupScore * 0.20 +
                settlementScore * 0.10
            ) *
            daylightAdjustment
            .coerceIn(
                0.0,
                1.0
            )
    }

    private fun calculateDistanceScore(
        zombie: Zombie,
        player: Player
    ): Double {

        val maxDistanceSquared =
            maximumRange *
                maximumRange

        if (
            maxDistanceSquared <=
                0.0
        ) {
            return 0.0
        }

        val distanceSquared =
            distanceSquared(
                zombie,
                player
            )

        return (
            1.0 -
                distanceSquared /
                    maxDistanceSquared
            )
            .coerceIn(
                0.0,
                1.0
            )
    }

    private fun calculateMemoryScore(
        playerId: UUID
    ): Double {

        val memory =
            playerMemory[
                playerId
            ]
                ?: return 0.0

        decayMemory(
            memory
        )

        return (
            memory.score /
                MAX_MEMORY_SCORE
            )
            .coerceIn(
                0.0,
                1.0
            )
    }

    private fun calculateGroupScore(
        zombie: Zombie,
        player: Player
    ): Double {

        val nearbyZombies =
            zombie.world
                .getNearbyEntities(
                    zombie.location,
                    GROUP_RADIUS,
                    GROUP_RADIUS,
                    GROUP_RADIUS
                )
                .filterIsInstance<Zombie>()
                .filter {
                    it !is ZombieVillager
                }
                .take(
                    MAX_GROUP_SIZE
                )

        if (
            nearbyZombies.isEmpty()
        ) {
            return 0.0
        }

        var sameTargetCount =
            0

        for (
            nearbyZombie in
            nearbyZombies
        ) {

            if (
                nearbyZombie.uniqueId ==
                    zombie.uniqueId
            ) {
                continue
            }

            val target =
                nearbyZombie.target as? Player
                    ?: continue

            if (
                target.uniqueId ==
                    player.uniqueId
            ) {
                sameTargetCount++
            }
        }

        return (
            sameTargetCount.toDouble() /
                MAX_GROUP_SIZE.toDouble()
            )
            .coerceIn(
                0.0,
                1.0
            )
    }

    private fun calculateSettlementScore(
        player: Player
    ): Double {

        val settlements =
            societyStateStore
                .current()
                .settlements
                .values

        for (
            settlement in
            settlements
        ) {

            if (
                settlement.worldName !=
                    player.world.name
            ) {
                continue
            }

            val dx =
                player.location.x -
                    settlement.centerX

            val dy =
                player.location.y -
                    settlement.centerY

            val dz =
                player.location.z -
                    settlement.centerZ

            val distanceSquared =
                dx * dx +
                    dy * dy +
                    dz * dz

            val radius =
                settlement.radius
                    .toDouble()

            if (
                distanceSquared <=
                    radius * radius
            ) {
                return 1.0
            }
        }

        return 0.0
    }

    private fun addMemory(
        playerId: UUID,
        amount: Double
    ) {

        val now =
            System.currentTimeMillis()

        playerMemory.compute(
            playerId
        ) { _, existing ->

            val memory =
                existing
                    ?: PlayerMemory(
                        score =
                            0.0,
                        lastUpdatedMillis =
                            now
                    )

            decayMemory(
                memory,
                now
            )

            memory.score =
                (
                    memory.score +
                        amount
                    )
                    .coerceAtMost(
                        MAX_MEMORY_SCORE
                    )

            memory.lastUpdatedMillis =
                now

            memory
        }
    }

    private fun decayMemory(
        memory: PlayerMemory,
        now: Long =
            System.currentTimeMillis()
    ) {

        val elapsed =
            now -
                memory.lastUpdatedMillis

        if (
            elapsed <=
                0L
        ) {
            return
        }

        val halfLives =
            elapsed.toDouble() /
                MEMORY_HALF_LIFE_MILLIS
                    .toDouble()

        val decayFactor =
            Math.pow(
                0.5,
                halfLives
            )

        memory.score *=
            decayFactor

        memory.lastUpdatedMillis =
            now
    }

    private fun isValidPlayer(
        player: Player
    ): Boolean {

        return player.isOnline &&
            !player.isDead &&
            player.gameMode !=
            GameMode.SPECTATOR
    }

    private fun distanceSquared(
        zombie: Zombie,
        player: Player
    ): Double {

        if (
            zombie.world.uid !=
                player.world.uid
        ) {
            return Double.MAX_VALUE
        }

        val dx =
            zombie.location.x -
                player.location.x

        val dy =
            zombie.location.y -
                player.location.y

        val dz =
            zombie.location.z -
                player.location.z

        return (
            dx * dx +
                dy * dy +
                dz * dz
            )
    }

    private fun debugEnabled(): Boolean {

        return plugin.config.getBoolean(
            "plugin.debug",
            false
        )
    }

    private data class TargetScore(
        val player: Player,
        val score: Double
    )
}
