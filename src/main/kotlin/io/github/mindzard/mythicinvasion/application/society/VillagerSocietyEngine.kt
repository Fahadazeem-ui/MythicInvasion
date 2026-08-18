package io.github.mindzard.mythicinvasion.application.society

import io.github.mindzard.mythicinvasion.domain.society.SettlementPopulationProfile
import io.github.mindzard.mythicinvasion.domain.society.VillagerIdentitySnapshot
import io.github.mindzard.mythicinvasion.domain.society.VillagerRoleSnapshot

class VillagerSocietyEngine {

    fun buildProfiles(
        villagers: List<VillagerIdentitySnapshot>
    ): List<SettlementPopulationProfile> {

        return villagers
            .groupBy { it.settlementId }
            .map { (settlementId, settlementVillagers) ->

                val adults =
                    settlementVillagers.count {
                        it.isAdult
                    }

                val children =
                    settlementVillagers.size -
                        adults

                val roleDistribution =
                    settlementVillagers
                        .groupingBy {
                            it.profession
                        }
                        .eachCount()
                        .entries
                        .sortedByDescending {
                            it.value
                        }
                        .map { entry ->

                            VillagerRoleSnapshot(
                                profession =
                                    entry.key,

                                count =
                                    entry.value
                            )
                        }

                val knownRelationships =
                    settlementVillagers.sumOf {
                        it.knownPlayerCount
                    }

                SettlementPopulationProfile(
                    settlementId =
                        settlementId,

                    population =
                        settlementVillagers.size,

                    adults =
                        adults,

                    children =
                        children,

                    roleDistribution =
                        roleDistribution,

                    knownPlayerRelationships =
                        knownRelationships,

                    updatedAtMillis =
                        settlementVillagers
                            .maxOfOrNull {
                                it.timestampMillis
                            }
                            ?: System.currentTimeMillis()
                )
            }
    }
}
