package io.github.mindzard.mythicinvasion.infrastructure.paper.ai

import io.github.mindzard.mythicinvasion.application.ai.AdaptiveTargetingEngine
import io.github.mindzard.mythicinvasion.application.ai.StrategyActionParser
import io.github.mindzard.mythicinvasion.application.ai.StrategyCooldownStore
import io.github.mindzard.mythicinvasion.application.ai.StrategyExecutionState
import io.github.mindzard.mythicinvasion.application.intelligence.BehaviourIntelligenceStore
import io.github.mindzard.mythicinvasion.application.society.SettlementSocialStore
import io.github.mindzard.mythicinvasion.application.society.SocietyStateStore
import io.github.mindzard.mythicinvasion.domain.ai.StrategyAction
import org.bukkit.GameMode
import org.bukkit.entity.Entity
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

        private const val GROUP_RADIUS =
            12.0

        private const val MAX_GROUP_SIZE =
            8

        private const val DAMAGE_MEMORY_GAIN =
            12.0

        private const val KILL_MEMORY_GAIN =
            24.0

        private const val PLAYER_DAMAGE_GAIN =
            6.0

        private const val MIN_PLAYER_SCORE =
            0.08

        private const val STRATEGIC_ADVANTAGE_MULTIPLIER =
            0.80

        private const val SUNLIGHT_RETARGET_PENALTY =
            0.18
    }

    private data class PlayerMemory(
        var score: Double,
        var lastUpdatedMillis: Long
    )

    private val playerMemory =
        ConcurrentHashMap<
            UUID,
            PlayerMemory
            >()

    private val mobCooldowns =
        ConcurrentHashMap<
            UUID,
            Long
            >()

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

        /*
         * Vanilla should still control the mob normally when
         * adaptive behaviour is disabled.
         */
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

        if (
            shouldAvoidStrategicRetarget(
                zombie
            )
        ) {
            return
        }

        val now =
            System.currentTimeMillis()

        if (
            !isCooldownReady(
                zombie,
                now
            )
        ) {
            return
        }

        /*
         * The event can fire with a non-player target.
         * In that case we still allow our local intelligence
         * to search for a valid player.
         */
        val currentPlayer =
            event.target as? Player
                ?.takeIf {
                    isValidPlayer(it)
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
                .toList()

        if (
            candidates.isEmpty()
        ) {
            return
        }

        val best =
            candidates
                .map { player ->

                    player to
                        scorePlayer(
                            zombie =
                                zombie,
                            player =
                                player,
                            currentTarget =
                                currentPlayer
                        )
                }
                .maxByOrNull {
                    it.second
                }
                ?: return

        val bestPlayer =
            best.first

        val bestScore =
            best.second

        val currentScore =
            currentPlayer?.let {
                scorePlayer(
                    zombie =
                        zombie,
                    player =
                        it,
                    currentTarget =
                        it
                )
            }
                ?: 0.0

        if (
            currentPlayer != null &&
            bestPlayer.uniqueId ==
                currentPlayer.uniqueId
        ) {
            return
        }

        val requiredAdvantage =
            if (
                adaptiveStrategyIsAggressive()
            ) {
                max(
                    0.04,
                    minimumAdvantage *
                        STRATEGIC_ADVANTAGE_MULTIPLIER
                )
            } else {
                minimumAdvantage
            }

        /*
         * Never swap targets for a tiny score difference.
         * This prevents zombie target ping-pong.
         */
        if (
            bestScore <
                MIN_PLAYER_SCORE
        ) {
            return
        }

        if (
            currentPlayer != null &&
            bestScore -
                currentScore <
                requiredAdvantage
        ) {
            return
        }

        event.setTarget(
            bestPlayer
        )

        mobCooldowns[
            zombie.uniqueId
        ] =
            now +
                cooldownMillis

        if (
            debugEnabled()
        ) {

            plugin.logger.info(
                "Adaptive zombie target: " +
                    "${currentPlayer?.name ?: "none"}" +
                    " -> " +
                    bestPlayer.name +
                    " | score=" +
                    "%.2f".format(
                        bestScore
                    )
            )
        }
    }

    @EventHandler(
        priority = EventPriority.MONITOR,
        ignoreCancelled = true
    )
    fun onZombieDamage(
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
            !isValidPlayer(player)
        ) {
            return
        }

        addMemory(
            playerId =
                player.uniqueId,
            amount =
                if (
                    event.damage >= 8.0
                ) {
                    DAMAGE_MEMORY_GAIN *
                        1.5
                } else {
                    DAMAGE_MEMORY_GAIN
                }
        )
    }

    @EventHandler(
        priority = EventPriority.MONITOR,
        ignoreCancelled = true
    )
    fun onPlayerHitByZombie(
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
            !isValidPlayer(player)
        ) {
            return
        }

        addMemory(
            playerId =
                player.uniqueId,
            amount =
                PLAYER_DAMAGE_GAIN
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

    private fun scorePlayer(
        zombie: Zombie,
        player: Player,
        currentTarget: Player?
    ): Double {

        val distanceScore =
            proximityScore(
                zombie,
                player
            )

        val memoryScore =
            memoryScore(
                player.uniqueId
            )

        val groupScore =
            groupPressureScore(
                zombie,
                player
            )

        val recentTargetBonus =
            if (
                currentTarget != null &&
                currentTarget.uniqueId ==
                    player.uniqueId
            ) {
                0.08
            } else {
                0.0
            }

        val settlementContextScore =
            settlementContextScore(
                player
            )

        val sunlightPenalty =
            if (
                isStrongDaylight(
                    zombie
                )
            ) {
                0.0
            } else {
                SUNLIGHT_RETARGET_PENALTY
            }

        val strategicPressure =
            if (
                adaptiveStrategyIsAggressive()
            ) {
                0.10
            } else {
                0.0
            }

        return (
            distanceScore * 0.32 +
                memoryScore * 0.33 +
                groupScore * 0.18 +
                settlementContextScore * 0.09 +
                recentTargetBonus +
                strategicPressure
            )
            .minus(
                sunlightPenalty *
                    0.05
            )
            .coerceIn(
                0.0,
                1.0
            )
    }

    private fun proximityScore(
        zombie: Zombie,
        player: Player
    ): Double {

        val maxDistanceSquared =
            maximumRange *
                maximumRange

        val distanceSquared =
            distanceSquared(
                zombie,
                player
            )

        if (
            maxDistanceSquared <=
                0.0
        ) {
            return 0.0
        }

        return (
            1.0 -
                (
                    distanceSquared /
                        maxDistanceSquared
                    )
            )
            .coerceIn(
                0.0,
                1.0
            )
    }

    private fun memoryScore(
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

    private fun groupPressureScore(
        zombie: Zombie,
        player: Player
    ): Double {

        val zombies =
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
            zombies.isEmpty()
        ) {
            return 0.0
        }

        var pressure =
            0

        for (
            other in zombies
        ) {

            if (
                other.uniqueId ==
                    zombie.uniqueId
            ) {
                continue
            }

            val target =
                other.target as? Player
                    ?: continue

            if (
                target.uniqueId ==
                    player.uniqueId
            ) {
                pressure++
            }
        }

        return (
            pressure.toDouble() /
                MAX_GROUP_SIZE.toDouble()
            )
            .coerceIn(
                0.0,
                1.0
            )
    }

    private fun settlementContextScore(
        player: Player
    ): Double {

        /*
         * This layer intentionally stays lightweight.
         * Settlement intelligence already exists elsewhere;
         * here we only reward players who are actively near a
         * known settlement area without forcing new dependencies.
         */
        val settlement =
            societyStateStore
                .current()
                .settlements
                .values
                .asSequence()
                .filter {
                    it.worldName ==
                        player.world.name
                }
                .map { settlement ->

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
                ?: return 0.0

        /*
         * A player near a settlement is slightly more important
         * during a strategic hostile-pressure state.
         */
        return if (
            settlement.population >
                0
        ) {
            1.0
        } else {
            0.0
        }
    }

    private fun adaptiveStrategyIsAggressive(): Boolean {

        val decision =
            strategyExecutionState
                .current()
                ?: return false

        val minimumConfidence =
            plugin.config
                .getDouble(
                    "ai.minimum-confidence",
                    0.65
                )
                .coerceIn(
                    0.0,
                    1.0
                )

        if (
            decision.confidence <
                minimumConfidence
        ) {
            return false
        }

        return decision
            .suggestedActions
            .asSequence()
            .map {
                actionParser.parse(
                    it
                )
            }
            .any {
                it ==
                    StrategyAction
                        .INCREASE_HOSTILE_PRESSURE ||
                    it ==
                    StrategyAction
                        .FOCUS_HIGH_PRESSURE_PLAYERS ||
                    it ==
                    StrategyAction
                        .ADAPTIVE_HOSTILE_TARGETING
            }
    }

    private fun addMemory(
        playerId: UUID,
        amount: Double
    ) {

        val now =
            System.currentTimeMillis()

        val memory =
            playerMemory.compute(
                playerId
            ) { _, existing ->

                val current =
                    existing
                        ?: PlayerMemory(
                            score =
                                0.0,
                            lastUpdatedMillis =
                                now
                        )

                decayMemory(
                    current,
                    now
                )

                current.score =
                    (
                        current.score +
                            amount
                        )
                        .coerceAtMost(
                            MAX_MEMORY_SCORE
                        )

                current.lastUpdatedMillis =
                    now

                current
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
            elapsed <= 0L
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

    private fun isCooldownReady(
        zombie: Zombie,
        now: Long
    ): Boolean {

        val nextAllowed =
            mobCooldowns[
                zombie.uniqueId
            ]
                ?: return true

        return now >=
            nextAllowed
    }

    private fun shouldAvoidStrategicRetarget(
        zombie: Zombie
    ): Boolean {

        /*
         * When exposed to strong daylight, keep vanilla pursuit
         * behavior stable rather than constantly calculating
         * ambitious target changes.
         */
        return isStrongDaylight(
            zombie
        ) &&
            zombie.isInDaylight
    }

    private fun isStrongDaylight(
        zombie: Zombie
    ): Boolean {

        return zombie.world.isDayTime &&
            zombie.location.block.lightFromSky >= 12
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
        entity: Entity,
        player: Player
    ): Double {

        if (
            entity.world.uid !=
                player.world.uid
        ) {
            return Double.MAX_VALUE
        }

        val dx =
            entity.location.x -
                player.location.x

        val dy =
            entity.location.y -
                player.location.y

        val dz =
            entity.location.z -
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
}
