package io.github.mindzard.mythicinvasion.domain.society

data class SettlementObservation(
    val settlementId: String,
    val name: String,
    val worldName: String,
    val centerX: Int,
    val centerY: Int,
    val centerZ: Int,
    val radius: Int,
    val villagerCount: Int,
    val ironGolemCount: Int,
    val lastUpdatedMillis: Long
)
