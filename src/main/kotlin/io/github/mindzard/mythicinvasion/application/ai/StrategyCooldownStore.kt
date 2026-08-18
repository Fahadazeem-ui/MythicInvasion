package io.github.mindzard.mythicinvasion.application.ai

import io.github.mindzard.mythicinvasion.domain.ai.StrategyAction
import java.util.concurrent.ConcurrentHashMap

class StrategyCooldownStore {

    private val cooldowns =
        ConcurrentHashMap<StrategyAction, Long>()

    fun isReady(
        action: StrategyAction,
        nowMillis: Long
    ): Boolean {

        val nextAllowed =
            cooldowns[action]
                ?: return true

        return nowMillis >=
            nextAllowed
    }

    fun put(
        action: StrategyAction,
        cooldownMillis: Long,
        nowMillis: Long
    ) {

        cooldowns[action] =
            nowMillis +
                cooldownMillis
                    .coerceAtLeast(1_000L)
    }

    fun clear() {
        cooldowns.clear()
    }
}
