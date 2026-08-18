package io.github.mindzard.mythicinvasion.domain.behaviour

import java.util.UUID

data class BehaviourEvent(
    val playerId: UUID,
    val playerName: String,
    val action: BehaviourAction,
    val worldName: String,
    val x: Int,
    val y: Int,
    val z: Int,
    val target: String?,
    val timestampMillis: Long
)

enum class BehaviourAction {
    JOIN,
    QUIT,
    MOVE,
    BLOCK_BREAK,
    BLOCK_PLACE,
    COMBAT
}
