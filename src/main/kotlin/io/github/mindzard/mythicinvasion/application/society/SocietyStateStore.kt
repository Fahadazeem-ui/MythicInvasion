package io.github.mindzard.mythicinvasion.application.society

import io.github.mindzard.mythicinvasion.domain.society.FactionState
import io.github.mindzard.mythicinvasion.domain.society.SettlementState
import io.github.mindzard.mythicinvasion.domain.society.SocietyWorldState
import java.util.concurrent.atomic.AtomicReference

class SocietyStateStore {

    private val state =
        AtomicReference(
            SocietyWorldState()
        )

    fun current(): SocietyWorldState {
        return state.get()
    }

    fun update(
        newState: SocietyWorldState
    ) {
        state.set(newState)
    }

    fun upsertFaction(
        faction: FactionState
    ) {
        state.updateAndGet { current ->

            current.copy(
                lastUpdatedMillis =
                    System.currentTimeMillis(),

                factions =
                    current.factions +
                        (
                            faction.factionId to
                                faction
                            )
            )
        }
    }

    fun removeFaction(
        factionId: String
    ) {
        state.updateAndGet { current ->

            current.copy(
                lastUpdatedMillis =
                    System.currentTimeMillis(),

                factions =
                    current.factions -
                        factionId
            )
        }
    }

    fun upsertSettlement(
        settlement: SettlementState
    ) {
        state.updateAndGet { current ->

            current.copy(
                lastUpdatedMillis =
                    System.currentTimeMillis(),

                settlements =
                    current.settlements +
                        (
                            settlement.settlementId to
                                settlement
                            )
            )
        }
    }

    fun removeSettlement(
        settlementId: String
    ) {
        state.updateAndGet { current ->

            current.copy(
                lastUpdatedMillis =
                    System.currentTimeMillis(),

                settlements =
                    current.settlements -
                        settlementId
            )
        }
    }

    fun reset() {
        state.set(
            SocietyWorldState()
        )
    }
}
