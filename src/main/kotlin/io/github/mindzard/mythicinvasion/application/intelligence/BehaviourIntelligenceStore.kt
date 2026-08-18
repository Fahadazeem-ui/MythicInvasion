package io.github.mindzard.mythicinvasion.application.intelligence

import io.github.mindzard.mythicinvasion.domain.behaviour.PlayerBehaviourProfile
import io.github.mindzard.mythicinvasion.domain.intelligence.PlayerIntelligenceProfile
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class BehaviourIntelligenceStore(
    private val engine: BehaviourIntelligenceEngine
) {

    private val profiles =
        ConcurrentHashMap<UUID, PlayerIntelligenceProfile>()

    fun update(
        behaviourProfile: PlayerBehaviourProfile
    ) {

        profiles.compute(
            behaviourProfile.playerId
        ) { _, previous ->

            engine.calculate(
                profile = behaviourProfile,
                previous = previous
            )
        }
    }

    fun get(
        playerId: UUID
    ): PlayerIntelligenceProfile? {
        return profiles[playerId]
    }

    fun snapshot(): List<PlayerIntelligenceProfile> {
        return profiles.values.toList()
    }

    fun remove(
        playerId: UUID
    ) {
        profiles.remove(playerId)
    }

    fun clear() {
        profiles.clear()
    }
}
