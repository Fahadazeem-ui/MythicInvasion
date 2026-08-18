package io.github.mindzard.mythicinvasion.domain.behaviour

import java.util.UUID

/**
 * Represents one meaningful player behaviour observation.
 *
 * Behaviour events are deliberately represented using plain Kotlin
 * data instead of Bukkit Player objects so that they can safely move
 * between the Minecraft server thread and asynchronous processing.
 */
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

/**
 * The categories of behaviour currently observed by the engine.
 *
 * This enum will grow later as the intelligence system becomes more
 * sophisticated.
 */
enum class BehaviourAction {

    /**
     * Player connected to the server.
     */
    JOIN,

    /**
     * Player disconnected from the server.
     */
    QUIT,

    /**
     * Player changed their block position.
     */
    MOVE,

    /**
     * Player broke a block.
     */
    BLOCK_BREAK,

    /**
     * Player placed a block.
     */
    BLOCK_PLACE,

    /**
     * Player dealt damage to another entity.
     */
    COMBAT
}
