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
import org.bukkit.entity.Pillager
import org.bukkit.entity.Player
import org.bukkit.entity.Zombie
import org.bukkit.entity.ZombieVillager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.plugin.java.JavaPlugin
import kotlin.math.max

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

    @EventHandler(
        priority = EventPriority.HIGHEST,
        ignoreCancelled = true
    )
    fun onMobTarget(
        event: EntityTargetLivingEntityEvent
    ) {

        val mob =
            event.entity

        val supported =
            mob is Zombie ||
                mob is Pillager

        if (!supported) {
            return
        }

        if (
            mob is ZombieVillager
        ) {
            return
        }

        val currentTarget =
            event.target as? Player
                ?: return

        if (
            !isValidPlayer(
                currentTarget
            )
        ) {
            return
        }

        if (
            currentTarget.world.uid !=
                mob.world.uid
        ) {
            return
        }

        val nowMillis =
            System.currentTimeMillis()

        val aiDecision =
            strategyExecutionState.current()

        val minimumAiConfidence =
            plugin.config
                .getDouble(
                    "ai.minimum-confidence",
                    0.65
                )
                .coerceIn(
                    0.0,
                    1.0
                )

        val aiActions =
            aiDecision
                ?.takeIf {
                    it.confidence >=
                        minimumAiConfidence
                }
                ?.suggestedActions
                ?.map {
                    actionParser.parse(it)
                }
                ?.toSet()
                ?: emptySet()

        val aiAdaptive =
            aiActions.contains(
                StrategyAction.FOCUS_HIGH_PRESSURE_PLAYERS
            ) ||
                aiActions.contains(
                    StrategyAction.ADAPTIVE_HOSTILE_TARGETING
                ) ||
                aiActions.contains(
                    StrategyAction.INCREASE_HOSTILE_PRESSURE
                )

        val adaptiveEnabled =
            plugin.config.getBoolean(
                "adaptive-behaviour.enabled",
                true
            )

        val targetingEnabled =
            plugin.config.getBoolean(
                "adaptive-behaviour.hostile-targeting.enabled",
                true
            )

        if (
            !adaptiveEnabled ||
            !targetingEnabled
        ) {
            return
        }

        /*
         * We only consider a retarget when the mob already has
         * a player target. This keeps vanilla villager/animal
         * targeting rules untouched.
         */
        val candidates =
            mob.world.players
                .asSequence()
                .filter {
                    isValidPlayer(it)
                }
                .filter {
                    it.uniqueId !=
                        currentTarget.uniqueId
                }
                .filter {
                    mob.location
                        .distanceSquared(
                            it.location
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

        val currentScore =
            scorePlayer(
                player =
                    currentTarget,

                mobX =
                    mob.location.x,

                mobY =
                    mob.location.y,

                mobZ =
                    mob.location.z,

                strategicPressure =
                    if (
                        aiAdaptive
                    ) {
                        1.0
                    } else {
                        0.0
                    }
            )

        val bestCandidate =
            candidates
                .asSequence()
                .map { candidate ->

                    candidate to
                        scorePlayer(
                            player =
                                candidate,

                            mobX =
                                mob.location.x,

                            mobY =
                                mob.location.y,

                            mobZ =
                                mob.location.z,

                            strategicPressure =
                                if (
                                    aiAdaptive
                                ) {
                                    1.0
                                } else {
                                    0.0
                                }
                        )
                }
                .maxByOrNull {
                    it.second.score
                }
                ?: return

        val bestPlayer =
            bestCandidate.first

        val bestScore =
            bestCandidate.second.score

        if (
            bestScore <=
                currentScore.score
        ) {
            return
        }

        val effectiveAdvantage =
            if (
                aiActions.contains(
                    StrategyAction.INCREASE_HOSTILE_PRESSURE
                )
            ) {
                max(
                    0.03,
                    minimumAdvantage * 0.60
                )
            } else if (
                aiAdaptive
            ) {
                max(
                    0.05,
                    minimumAdvantage * 0.80
                )
            } else {
                minimumAdvantage
            }

        if (
            bestScore -
                currentScore.score <
                effectiveAdvantage
        ) {
            return
        }

        val cooldownAction =
            StrategyAction
                .ADAPTIVE_HOSTILE_TARGETING

        if (
            !cooldownStore.isReady(
                cooldownAction,
                nowMillis
            )
        ) {
            return
        }

        event.setTarget(
            bestPlayer
        )

        cooldownStore.put(
            action =
                cooldownAction,

            cooldownMillis =
                cooldownMillis,

            nowMillis =
                nowMillis
        )

        if (
            plugin.config
                .getBoolean(
                    "plugin.debug",
                    false
                )
        ) {

            plugin.logger.info(
                "Adaptive target: " +
                    "${mob.type} " +
                    "switched from " +
                    currentTarget.name +
                    " to " +
                    bestPlayer.name +
                    " (" +
                    "%.2f".format(bestScore) +
                    " > " +
                    "%.2f".format(
                        currentScore.score
                    ) +
                    ")"
            )
        }
    }

    private fun scorePlayer(
        player: Player,
        mobX: Double,
        mobY: Double,
        mobZ: Double,
        strategicPressure: Double
    ) = adaptiveTargetingEngine.score(
        playerId =
            player.uniqueId,

        distanceSquared =
            squareDistance(
                player.x,
                player.y,
                player.z,
                mobX,
                mobY,
                mobZ
            ),

        maximumDistance =
            maximumRange,

        intelligenceProfile =
            intelligenceStore.get(
                player.uniqueId
            ),

        socialThreat =
            findSocialThreat(
                player
            ),

        activityLevel =
            intelligenceStore
                .get(
                    player.uniqueId
                )
                ?.score(
                    io.github.mindzard.mythicinvasion
                        .domain.intelligence.BehaviourArchetype.EXPLORER
                )
                ?: 0.0,

        strategicPressure =
            strategicPressure
    )

    private fun findSocialThreat(
        player: Player
    ): Double {

        val settlements =
            societyStateStore
                .current()
                .settlements
                .values

        val nearestSettlement =
            settlements
                .asSequence()
                .filter {
                    it.worldName ==
                        player.world.name
                }
                .map { settlement ->

                    val dx =
                        player.location.blockX -
                            settlement.centerX

                    val dy =
                        player.location.blockY -
                            settlement.centerY

                    val dz =
                        player.location.blockZ -
                            settlement.centerZ

                    val distanceSquared =
                        dx.toDouble() * dx +
                            dy.toDouble() * dy +
                            dz.toDouble() * dz

                    settlement to
                        distanceSquared
                }
                .filter { (settlement, distanceSquared) ->
                    distanceSquared <=
                        settlement.radius *
                        settlement.radius
                }
                .minByOrNull {
                    it.second
                }
                ?.first
                ?: return 0.0

        val social =
            settlementSocialStore.get(
                nearestSettlement.settlementId
            )
                ?: return 0.0

        return (
            social.hostilePlayers[
                player.uniqueId
            ]
                ?: 0.0
            )
            .coerceIn(
                0.0,
                1.0
            )
    }

    private fun isValidPlayer(
        player: Player
    ): Boolean {

        return player.isOnline &&
            !player.isDead &&
            player.gameMode !=
            GameMode.SPECTATOR
    }

    private fun squareDistance(
        x1: Double,
        y1: Double,
        z1: Double,
        x2: Double,
        y2: Double,
        z2: Double
    ): Double {

        val dx =
            x1 - x2

        val dy =
            y1 - y2

        val dz =
            z1 - z2

        return (
            dx * dx +
                dy * dy +
                dz * dz
            )
    }
}
