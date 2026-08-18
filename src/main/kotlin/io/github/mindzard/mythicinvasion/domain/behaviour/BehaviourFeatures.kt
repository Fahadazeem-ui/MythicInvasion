package io.github.mindzard.mythicinvasion.domain.behaviour

/**
 * Derived behavioural signals for a player.
 *
 * Every value is normalized to the range 0.0..1.0.
 *
 * These are heuristic signals, not machine-learning predictions.
 * Later, richer context and historical data will make them more
 * sophisticated.
 */
data class BehaviourFeatures(
    val miningTendency: Double = 0.0,
    val buildingTendency: Double = 0.0,
    val combatTendency: Double = 0.0,
    val movementTendency: Double = 0.0,
    val activityLevel: Double = 0.0
) {

    /**
     * Returns the strongest currently observed behavioural tendency.
     */
    fun dominantBehaviour(): BehaviourType {
        val candidates = mapOf(
            BehaviourType.MINING to miningTendency,
            BehaviourType.BUILDING to buildingTendency,
            BehaviourType.COMBAT to combatTendency,
            BehaviourType.MOVEMENT to movementTendency
        )

        return candidates.maxByOrNull { it.value }?.key
            ?: BehaviourType.UNKNOWN
    }
}

enum class BehaviourType {
    MINING,
    BUILDING,
    COMBAT,
    MOVEMENT,
    UNKNOWN
}
