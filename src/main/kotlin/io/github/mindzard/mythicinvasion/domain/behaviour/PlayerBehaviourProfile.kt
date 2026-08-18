package io.github.mindzard.mythicinvasion.domain.behaviour

import java.util.UUID

data class PlayerBehaviourProfile(
    val playerId: UUID,
    val totalEvents: Long = 0L,
    val joins: Long = 0L,
    val quits: Long = 0L,
    val movements: Long = 0L,
    val blocksBroken: Long = 0L,
    val blocksPlaced: Long = 0L,
    val combatActions: Long = 0L,
    val lastSeenMillis: Long = 0L,
    val features: BehaviourFeatures = BehaviourFeatures()
)
