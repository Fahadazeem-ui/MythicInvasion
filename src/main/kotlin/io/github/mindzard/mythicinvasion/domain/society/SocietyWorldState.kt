package io.github.mindzard.mythicinvasion.domain.society

data class SocietyWorldState(
    val lastUpdatedMillis: Long = 0L,
    val factions: Map<String, FactionState> = emptyMap(),
    val settlements: Map<String, SettlementState> = emptyMap()
)
