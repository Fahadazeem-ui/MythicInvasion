package io.github.mindzard.mythicinvasion.domain.behaviour

import java.util.UUID

data class PlayerBehaviourProfile(
    val playerId: UUID,

    /*
     * Lifetime counters.
     *
     * These are historical statistics and are not used directly
     * as current behavioural intelligence signals.
     */
    val totalEvents: Long = 0L,
    val joins: Long = 0L,
    val quits: Long = 0L,
    val movements: Long = 0L,
    val blocksBroken: Long = 0L,
    val blocksPlaced: Long = 0L,
    val combatActions: Long = 0L,

    /*
     * Time-weighted behavioural counters.
     *
     * These continuously decay as observations become old.
     */
    val weightedMovements: Double = 0.0,
    val weightedBlocksBroken: Double = 0.0,
    val weightedBlocksPlaced: Double = 0.0,
    val weightedCombatActions: Double = 0.0,

    /*
     * Last event processed for this player.
     */
    val lastSeenMillis: Long = 0L,

    /*
     * Last timestamp at which the weighted state itself was decayed.
     */
    val lastDecayMillis: Long = 0L,

    /*
     * Derived behavioural features.
     */
    val features: BehaviourFeatures = BehaviourFeatures()
)
