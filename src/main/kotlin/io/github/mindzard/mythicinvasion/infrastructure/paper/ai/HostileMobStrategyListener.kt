package io.github.mindzard.mythicinvasion.infrastructure.paper.ai

import io.github.mindzard.mythicinvasion.application.ai.StrategyActionParser
import io.github.mindzard.mythicinvasion.application.ai.StrategyCooldownStore
import io.github.mindzard.mythicinvasion.application.ai.StrategyExecutionState
import io.github.mindzard.mythicinvasion.domain.ai.StrategyAction
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityTargetLivingEntityEvent

class HostileMobStrategyListener(
    private val executionState: StrategyExecutionState,
    private val actionParser: StrategyActionParser,
    private val cooldownStore: StrategyCooldownStore
) : Listener {

    companion object {

        private const val TARGET_SELECTION_COOLDOWN =
            5_000L

        private const val MAX_TARGET_DISTANCE_SQUARED =
            48.0 * 48.0
    }

    @EventHandler(
        priority = EventPriority.MONITOR,
        ignoreCancelled = true
    )
    fun onMobTarget(
        event: EntityTargetLivingEntityEvent
    ) {

        val monster =
            event.entity as? Monster
                ?: return

        val decision =
            executionState.current()
                ?: return

        if (
            decision.confidence < 0.65
        ) {
            return
        }

        if (
            decision.priority < 50
        ) {
            return
        }

        val action =
            decision.suggestedActions
                .asSequence()
                .map {
                    actionParser.parse(it)
                }
                .firstOrNull {
                    it != StrategyAction.NONE
                }
                ?: return

        if (
            action !=
                StrategyAction.FOCUS_HIGH_PRESSURE_PLAYERS
        ) {
            return
        }

        val player =
            event.target as? Player
                ?: return

        if (
            !player.isOnline ||
            player.isDead
        ) {
            return
        }

        if (
            player.world.uid !=
                monster.world.uid
        ) {
            return
        }

        val distanceSquared =
            monster.location
                .distanceSquared(
                    player.location
                )

        if (
            distanceSquared >
                MAX_TARGET_DISTANCE_SQUARED
        ) {
            return
        }

        val nowMillis =
            System.currentTimeMillis()

        if (
            !cooldownStore.isReady(
                action,
                nowMillis
            )
        ) {
            return
        }

        /*
         * The first execution mode is intentionally conservative:
         * preserve vanilla target selection and confirm that the
         * AI strategy permits the hostile target.
         *
         * Future strategy executors can replace this with ranked
         * target selection based on actual player pressure.
         */
        cooldownStore.put(
            action =
                action,

            cooldownMillis =
                TARGET_SELECTION_COOLDOWN,

            nowMillis =
                nowMillis
        )
    }
}
