package io.github.mindzard.mythicinvasion.infrastructure.paper.player

import io.github.mindzard.mythicinvasion.application.behaviour.BehaviourEventBuffer
import io.github.mindzard.mythicinvasion.domain.behaviour.BehaviourAction
import io.github.mindzard.mythicinvasion.domain.behaviour.BehaviourEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerBehaviourListener(
    private val buffer: BehaviourEventBuffer
) : Listener {

    @EventHandler(
        priority = EventPriority.MONITOR,
        ignoreCancelled = true
    )
    fun onPlayerJoin(
        event: PlayerJoinEvent
    ) {
        record(
            player = event.player,
            action = BehaviourAction.JOIN,
            target = null
        )
    }

    @EventHandler(
        priority = EventPriority.MONITOR,
        ignoreCancelled = true
    )
    fun onPlayerQuit(
        event: PlayerQuitEvent
    ) {
        record(
            player = event.player,
            action = BehaviourAction.QUIT,
            target = null
        )
    }

    @EventHandler(
        priority = EventPriority.MONITOR,
        ignoreCancelled = true
    )
    fun onBlockBreak(
        event: BlockBreakEvent
    ) {
        record(
            player = event.player,
            action = BehaviourAction.BLOCK_BREAK,
            target =
                event.block.type.key.toString()
        )
    }

    @EventHandler(
        priority = EventPriority.MONITOR,
        ignoreCancelled = true
    )
    fun onBlockPlace(
        event: BlockPlaceEvent
    ) {
        record(
            player = event.player,
            action = BehaviourAction.BLOCK_PLACE,
            target =
                event.blockPlaced.type.key.toString()
        )
    }

    @EventHandler(
        priority = EventPriority.MONITOR,
        ignoreCancelled = true
    )
    fun onEntityDamage(
        event: EntityDamageByEntityEvent
    ) {

        val player =
            event.damager as? Player
                ?: return

        record(
            player = player,
            action = BehaviourAction.COMBAT,
            target =
                event.entity.type.key.toString()
        )
    }

    @EventHandler(
        priority = EventPriority.MONITOR,
        ignoreCancelled = true
    )
    fun onPlayerMove(
        event: PlayerMoveEvent
    ) {

        val from =
            event.from

        val to =
            event.to

        if (
            from.blockX == to.blockX &&
            from.blockY == to.blockY &&
            from.blockZ == to.blockZ &&
            from.world.uid == to.world.uid
        ) {
            return
        }

        record(
            player = event.player,
            action = BehaviourAction.MOVE,
            target = null
        )
    }

    private fun record(
        player: Player,
        action: BehaviourAction,
        target: String?
    ) {

        val location =
            player.location

        val world =
            location.world

        buffer.add(
            BehaviourEvent(
                playerId =
                    player.uniqueId,

                playerName =
                    player.name,

                action =
                    action,

                worldName =
                    world.name,

                x =
                    location.blockX,

                y =
                    location.blockY,

                z =
                    location.blockZ,

                target =
                    target,

                timestampMillis =
                    System.currentTimeMillis()
            )
        )
    }
}
