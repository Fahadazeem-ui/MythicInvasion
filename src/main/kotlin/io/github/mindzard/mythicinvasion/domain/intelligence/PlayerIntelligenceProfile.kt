package io.github.mindzard.mythicinvasion.domain.intelligence

import java.util.UUID

data class PlayerIntelligenceProfile(
    val playerId: UUID,
    val archetypeScores: Map<BehaviourArchetype, Double> = emptyMap(),
    val confidence: Double = 0.0,
    val dominantArchetype: BehaviourArchetype? = null,
    val sampleCount: Long = 0L,
    val lastUpdatedMillis: Long = 0L
) {

    fun score(
        archetype: BehaviourArchetype
    ): Double {
        return archetypeScores[archetype] ?: 0.0
    }
}
