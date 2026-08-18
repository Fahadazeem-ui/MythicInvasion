package io.github.mindzard.mythicinvasion.application.society

import io.github.mindzard.mythicinvasion.domain.society.PillagerStrategyState
import java.util.concurrent.atomic.AtomicReference

class PillagerStrategyStore {

    private val state =
        AtomicReference(
            PillagerStrategyState()
        )

    fun current(): PillagerStrategyState {
        return state.get()
    }

    fun update(
        newState: PillagerStrategyState
    ) {
        state.set(
            newState
        )
    }

    fun clear() {
        state.set(
            PillagerStrategyState()
        )
    }
}
