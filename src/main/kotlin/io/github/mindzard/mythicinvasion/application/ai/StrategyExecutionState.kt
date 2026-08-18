package io.github.mindzard.mythicinvasion.application.ai

import io.github.mindzard.mythicinvasion.domain.ai.AiDecision
import java.util.concurrent.atomic.AtomicReference

class StrategyExecutionState {

    private val latestDecision =
        AtomicReference<AiDecision?>(
            null
        )

    fun update(
        decision: AiDecision
    ) {
        latestDecision.set(
            decision
        )
    }

    fun current(): AiDecision? {
        return latestDecision.get()
    }

    fun clear() {
        latestDecision.set(
            null
        )
    }
}
