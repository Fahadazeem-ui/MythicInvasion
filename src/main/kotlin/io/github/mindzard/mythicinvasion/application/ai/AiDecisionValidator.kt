package io.github.mindzard.mythicinvasion.application.ai

import io.github.mindzard.mythicinvasion.domain.ai.AiDecision

class AiDecisionValidator {

    fun validate(
        decision: AiDecision
    ): AiDecision? {

        if (
            decision.strategyId.isBlank()
        ) {
            return null
        }

        if (
            decision.summary.isBlank()
        ) {
            return null
        }

        if (
            decision.reasoning.isBlank()
        ) {
            return null
        }

        if (
            decision.priority !in 0..100
        ) {
            return null
        }

        if (
            decision.confidence !in 0.0..1.0
        ) {
            return null
        }

        if (
            decision.suggestedActions.size > 10
        ) {
            return null
        }

        if (
            decision.suggestedActions.any {
                it.isBlank() ||
                    it.length > 250
            }
        ) {
            return null
        }

        return decision.copy(
            strategyId =
                decision.strategyId
                    .trim()
                    .take(100),

            summary =
                decision.summary
                    .trim()
                    .take(500),

            reasoning =
                decision.reasoning
                    .trim()
                    .take(2_000),

            suggestedActions =
                decision.suggestedActions
                    .take(10)
                    .map {
                        it.trim()
                            .take(250)
                    }
        )
    }
}
