package io.github.mindzard.mythicinvasion.domain.society

data class SettlementPopulationProfile(
    val settlementId: String,
    val population: Int,
    val adults: Int,
    val children: Int,
    val roleDistribution: List<VillagerRoleSnapshot>,
    val knownPlayerRelationships: Int,
    val updatedAtMillis: Long
)
