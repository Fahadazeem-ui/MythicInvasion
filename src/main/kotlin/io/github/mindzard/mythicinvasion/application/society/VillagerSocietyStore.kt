package io.github.mindzard.mythicinvasion.application.society

import io.github.mindzard.mythicinvasion.domain.society.SettlementPopulationProfile
import java.util.concurrent.ConcurrentHashMap

class VillagerSocietyStore {

    private val profiles =
        ConcurrentHashMap<String, SettlementPopulationProfile>()

    fun replaceAll(
        newProfiles: Collection<SettlementPopulationProfile>
    ) {

        val replacement =
            newProfiles.associateBy {
                it.settlementId
            }

        profiles.clear()
        profiles.putAll(
            replacement
        )
    }

    fun get(
        settlementId: String
    ): SettlementPopulationProfile? {
        return profiles[
            settlementId
        ]
    }

    fun snapshot(): List<SettlementPopulationProfile> {
        return profiles.values.toList()
    }

    fun clear() {
        profiles.clear()
    }
}
