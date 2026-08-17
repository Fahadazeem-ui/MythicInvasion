package io.github.mindzard.mythicinvasion.infrastructure.paper.player

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.entity.Player

class PlayerBehaviourListener : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        record(
            player = player,
            action = "JOIN",
            details = "Player joined the server."
        )
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player

        record(
            player = player,
            action = "QUIT",
            details = "Player left the server."
        )
    }

    @EventHandler(
        priority = EventPriority.MONITOR,
        ignoreCancelled = true
    )
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val block = event.block

        record(
            player = player,
            action = "BLOCK_BREAK",
            details = "block=${block.type.key}"
        )
    }

    @EventHandler(
        priority = EventPriority.MONITOR,
        ignoreCancelled = true
    )
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        val block = event.blockPlaced

        record(
            player = player,
            action = "BLOCK_PLACE",
            details = "block=${block.type.key}"
        )
    }

    @EventHandler(
        priority = EventPriority.MONITOR,
        ignoreCancelled = true
    )
    fun onEntityDamage(event: EntityDamageByEntityEvent) {
        val damager = event.damager

        if (damager !is Player) {
            return
        }

        record(
            player = damager,
            action = "COMBAT",
            details = "target=${event.entity.type.key}"
        )
    }

    @EventHandler(
        priority = EventPriority.MONITOR,
        ignoreCancelled = true
    )
    fun onPlayerMove(event: PlayerMoveEvent) {
        val from = event.from
        val to = event.to ?: return

        /*
         * PlayerMoveEvent can fire extremely frequently.
         *
         * We intentionally ignore movement where the player only
         * changed their camera direction.
         *
         * This means the behaviour system receives movement information
         * only when the player actually changes block position.
         */
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
            action = "MOVE",
            details = "world=${to.world.name},x=${to.blockX},y=${to.blockY},z=${to.blockZ}"
        )
    }

    private fun record(
        player: Player,
        action: String,
        details: String
    ) {
        /*
         * This is intentionally lightweight.
         *
         * We are NOT doing database writes,
         * network requests, AI calls, or expensive calculations
         * inside Minecraft event handlers.
         *
         * The next behaviour-system layer will consume these events
         * through a dedicated buffer and asynchronous processor.
         */
        player.server.pluginManager
            .getPlugin("MythicInvasion")
            ?.logger
            ?.fine(
                "BehaviourEvent player=${player.uniqueId} " +
                    "name=${player.name} " +
                    "action=$action " +
                    "details=$details"
            )
    }
}
