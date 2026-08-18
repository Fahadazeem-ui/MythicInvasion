package io.github.mindzard.mythicinvasion.application.society

import io.github.mindzard.mythicinvasion.domain.society.SettlementSocialProfile
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class SettlementSocialStore {

    private val profiles =
        ConcurrentHashMap<String, SettlementSocialProfile>()

    fun replaceAll(
        newProfiles:
            Collection<SettlementSocialProfile>
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
    ): SettlementSocialProfile? {
        return profiles[
            settlementId
        ]
    }

    fun findStanding(
        settlementId: String,
        playerId: UUID
    ): Double? {

        val profile =
            profiles[
                settlementId
            ]
                ?: return null

        return profile.trustedPlayers[playerId]
            ?: profile.neutralPlayers[playerId]
            ?: profile.hostilePlayers[playerId]
    }

    fun snapshot():
        List<SettlementSocialProfile> {
        return profiles.values.toList()
    }

    fun clear() {
        profiles.clear()
    }
}
