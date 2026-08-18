package io.github.mindzard.mythicinvasion.application.world

import io.github.mindzard.mythicinvasion.domain.world.WorldIntelligenceState
import java.util.concurrent.atomic.AtomicReference

class WorldStateStore {

    private val state =
        AtomicReference(
            WorldIntelligenceState()
        )

    fun update(
        newState: WorldIntelligenceState
    ) {
        state.set(newState)
    }

    fun current():
        WorldIntelligenceState {
        return state.get()
    }

    fun reset() {
        state.set(
            WorldIntelligenceState()
        )
    }
}
