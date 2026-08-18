package io.github.mindzard.mythicinvasion.infrastructure.paper.ecosystem

import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Animals
import org.bukkit.entity.Entity
import org.bukkit.entity.Horse
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.entity.Wolf
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

class AnimalBehaviourListener(
    private val plugin: JavaPlugin
) : Listener {

    companion object {

        private const val THINK_INTERVAL_TICKS =
            40L

        private const val DANGER_RADIUS =
            12.0

        private const val PLAYER_APPROACH_RADIUS =
            8.0

        private const val HERD_RADIUS =
            10.0

        private const val HERD_MOVE_RADIUS =
            7.0

        private const val FLEE_SPEED =
            0.95

        private const val FOLLOW_SPEED =
            0.65

        private const val HERD_SPEED =
            0.55

        private const val ACTION_COOLDOWN_MILLIS =
            4_000L

        private const val MEMORY_HALF_LIFE_MILLIS =
            90_000L

        private const val MAX_DANGER_MEMORY =
            100.0
    }

    private data class DangerMemory(
        var score: Double,
        var updatedAtMillis: Long
    )

    private val dangerMemory =
        ConcurrentHashMap<UUID, DangerMemory>()

    private val actionCooldowns =
        ConcurrentHashMap<UUID, Long>()

    private val task: BukkitTask =
        plugin.server.scheduler.runTaskTimer(
            plugin,
            Runnable {
                think()
            },
            THINK_INTERVAL_TICKS,
            THINK_INTERVAL_TICKS
        )

    private fun think() {

        val now =
            System.currentTimeMillis()

        for (world in plugin.server.worlds) {

            val animals =
                world.entities
                    .asSequence()
                    .filterIsInstance<Animals>()
                    .filter {
                        !it.isDead
                    }
                    .filter {
                        it.isValid
                    }
                    .toList()

            for (animal in animals) {

                if (
                    isOnCooldown(
                        animal,
                        now
                    )
                ) {
                    continue
                }

                val danger =
                    findNearestDanger(
                        animal
                    )

                if (danger != null) {

                    rememberDanger(
                        danger
                    )

                    flee(
                        animal,
                        danger
                    )

                    setCooldown(
                        animal,
                        now
                    )

                    continue
                }

                val trustedPlayer =
                    findNearbyPlayer(
                        animal
                    )

                if (
                    trustedPlayer != null &&
                    animal is Wolf
                ) {

                    /*
                     * Wolves retain their native taming/owner AI.
                     * We only provide a small awareness/follow signal.
                     */
                    val owner =
                        animal.owner

                    if (
                        owner != null &&
                        owner.uniqueId ==
                            trustedPlayer.uniqueId
                    ) {

                        approachPlayer(
                            animal,
                            trustedPlayer
                        )

                        setCooldown(
                            animal,
                            now
                        )

                        continue
                    }
                }

                if (
                    animal is Horse &&
                    animal.isTamed
                ) {
                    continue
                }

                val herdMate =
                    findNearestHerdMate(
                        animal
                    )

                if (
                    herdMate != null
                ) {

                    herdMove(
                        animal,
                        herdMate
                    )

                    setCooldown(
                        animal,
                        now
                    )
                }
            }
        }

        decayMemory(
            now
        )
    }

    @EventHandler(
        priority = EventPriority.MONITOR,
        ignoreCancelled = true
    )
    fun onAnimalDamaged(
        event: EntityDamageByEntityEvent
    ) {

        val animal =
            event.entity as? Animals
                ?: return

        if (
            animal.isDead
        ) {
            return
        }

        val attacker =
            event.damager

        if (
            attacker is Player
        ) {

            addDangerMemory(
                attacker.uniqueId,
                20.0
            )
        }

        if (
            attacker is Monster
        ) {

            addDangerMemory(
                attacker.uniqueId,
                30.0
            )
        }
    }

    @EventHandler(
        priority = EventPriority.HIGHEST,
        ignoreCancelled = true
    )
    fun onAnimalTarget(
        event: EntityTargetLivingEntityEvent
    ) {

        val animal =
            event.entity as? Animals
                ?: return

        /*
         * Animals should not unexpectedly become aggressive
         * toward players. Preserve their native target when it
         * makes sense, but remove clearly invalid player targets.
         */
        val target =
            event.target
                ?: return

        if (
            target is Player &&
            !isValidPlayer(target)
        ) {

            event.target = null
        }
    }

    @EventHandler(
        priority = EventPriority.MONITOR,
        ignoreCancelled = true
    )
    fun onPlayerInteract(
        event: PlayerInteractEntityEvent
    ) {

        val animal =
            event.rightClicked as? Animals
                ?: return

        val player =
            event.player

        if (
            !isValidPlayer(
                player
            )
        ) {
            return
        }

        /*
         * Positive interaction reduces fear of the player.
         */
        dangerMemory.remove(
            player.uniqueId
        )

        if (
            animal is Wolf
        ) {

            val owner =
                animal.owner

            if (
                owner?.uniqueId ==
                    player.uniqueId
            ) {

                animal.lookAt(
                    player
                )
            }
        }
    }

    private fun findNearestDanger(
        animal: Animals
    ): LivingEntity? {

        return animal.world
            .getNearbyEntities(
                animal.location,
                DANGER_RADIUS,
                DANGER_RADIUS,
                DANGER_RADIUS
            )
            .asSequence()
            .filterIsInstance<LivingEntity>()
            .filter {
                it != animal
            }
            .filter {
                !it.isDead
            }
            .filter {
                isDangerous(
                    it
                )
            }
            .minByOrNull {
                distanceSquared(
                    animal,
                    it
                )
            }
    }

    private fun isDangerous(
        entity: LivingEntity
    ): Boolean {

        if (
            entity is Monster
        ) {
            return true
        }

        if (
            entity is Player
        ) {

            return isPlayerDangerous(
                entity
            )
        }

        return false
    }

    private fun isPlayerDangerous(
        player: Player
    ): Boolean {

        if (
            !isValidPlayer(
                player
            )
        ) {
            return false
        }

        val memory =
            dangerMemory[
                player.uniqueId
            ]
                ?: return false

        decayMemory(
            memory
        )

        return memory.score >=
            15.0
    }

    private fun findNearbyPlayer(
        animal: Animals
    ): Player? {

        return animal.world
            .getNearbyPlayers(
                PLAYER_APPROACH_RADIUS
            )
            .asSequence()
            .filter {
                isValidPlayer(
                    it
                )
            }
            .filter {
                !isPlayerDangerous(
                    it
                )
            }
            .minByOrNull {
                distanceSquared(
                    animal,
                    it
                )
            }
    }

    private fun findNearestHerdMate(
        animal: Animals
    ): Animals? {

        return animal.world
            .getNearbyEntities(
                animal.location,
                HERD_RADIUS,
                HERD_RADIUS,
                HERD_RADIUS
            )
            .asSequence()
            .filterIsInstance<Animals>()
            .filter {
                it.uniqueId !=
                    animal.uniqueId
            }
            .filter {
                !it.isDead
            }
            .filter {
                it.type ==
                    animal.type
            }
            .minByOrNull {
                distanceSquared(
                    animal,
                    it
                )
            }
    }

    private fun flee(
        animal: Animals,
        danger: LivingEntity
    ) {

        val target =
            calculateFleeLocation(
                animal.location,
                danger.location
            )

        if (
            animal is org.bukkit.entity.Mob
        ) {

            animal.pathfinder.moveTo(
                target,
                FLEE_SPEED
            )

            animal.lookAt(
                danger
            )
        }
    }

    private fun approachPlayer(
        animal: Animals,
        player: Player
    ) {

        if (
            animal !is org.bukkit.entity.Mob
        ) {
            return
        }

        animal.pathfinder.moveTo(
            player.location,
            FOLLOW_SPEED
        )

        animal.lookAt(
            player
        )
    }

    private fun herdMove(
        animal: Animals,
        herdMate: Animals
    ) {

        if (
            animal !is org.bukkit.entity.Mob
        ) {
            return
        }

        val target =
            calculateHerdLocation(
                animal.location,
                herdMate.location
            )

        animal.pathfinder.moveTo(
            target,
            HERD_SPEED
        )
    }

    private fun calculateFleeLocation(
        animal: Location,
        danger: Location
    ): Location {

        var dx =
            animal.x -
                danger.x

        var dz =
            animal.z -
                danger.z

        val length =
            sqrt(
                dx * dx +
                    dz * dz
            )

        if (
            length <=
                0.001
        ) {
            dx = 1.0
            dz = 0.0
        } else {
            dx /=
                length
            dz /=
                length
        }

        return animal.clone().apply {

            x +=
                dx * HERD_MOVE_RADIUS

            z +=
                dz * HERD_MOVE_RADIUS
        }
    }

    private fun calculateHerdLocation(
        animal: Location,
        herdMate: Location
    ): Location {

        val midpointX =
            (
                animal.x +
                    herdMate.x
                ) /
                2.0

        val midpointZ =
            (
                animal.z +
                    herdMate.z
                ) /
                2.0

        return animal.clone().apply {

            x +=
                (
                    midpointX -
                        animal.x
                    ) *
                    0.5

            z +=
                (
                    midpointZ -
                        animal.z
                    ) *
                    0.5
        }
    }

    private fun rememberDanger(
        danger: LivingEntity
    ) {

        val id =
            danger.uniqueId

        addDangerMemory(
            id,
            if (
                danger is Monster
            ) {
                15.0
            } else {
                10.0
            }
        )
    }

    private fun addDangerMemory(
        id: UUID,
        amount: Double
    ) {

        val now =
            System.currentTimeMillis()

        dangerMemory.compute(
            id
        ) { _, existing ->

            val memory =
                existing
                    ?: DangerMemory(
                        score = 0.0,
                        updatedAtMillis = now
                    )

            decayMemory(
                memory,
                now
            )

            memory.score =
                (
                    memory.score +
                        amount
                    )
                    .coerceAtMost(
                        MAX_DANGER_MEMORY
                    )

            memory.updatedAtMillis =
                now

            memory
        }
    }

    private fun decayMemory(
        now: Long
    ) {

        dangerMemory.values
            .forEach {
                decayMemory(
                    it,
                    now
                )
            }

        dangerMemory.entries.removeIf {
            it.value.score < 0.5
        }
    }

    private fun decayMemory(
        memory: DangerMemory,
        now: Long
    ) {

        val elapsed =
            now -
                memory.updatedAtMillis

        if (
            elapsed <=
                0L
        ) {
            return
        }

        val halfLives =
            elapsed.toDouble() /
                MEMORY_HALF_LIFE_MILLIS

        val factor =
            Math.pow(
                0.5,
                halfLives
            )

        memory.score *=
            factor

        memory.updatedAtMillis =
            now
    }

    private fun isOnCooldown(
        animal: Animals,
        now: Long
    ): Boolean {

        val next =
            actionCooldowns[
                animal.uniqueId
            ]
                ?: return false

        return now <
            next
    }

    private fun setCooldown(
        animal: Animals,
        now: Long
    ) {

        actionCooldowns[
            animal.uniqueId
        ] =
            now +
                ACTION_COOLDOWN_MILLIS
    }

    private fun isValidPlayer(
        player: Player
    ): Boolean {

        return player.isOnline &&
            !player.isDead &&
            player.gameMode !=
            GameMode.SPECTATOR
    }

    private fun distanceSquared(
        first: Entity,
        second: Entity
    ): Double {

        if (
            first.world.uid !=
                second.world.uid
        ) {
            return Double.MAX_VALUE
        }

        val dx =
            first.location.x -
                second.location.x

        val dy =
            first.location.y -
                second.location.y

        val dz =
            first.location.z -
                second.location.z

        return (
            dx * dx +
                dy * dy +
                dz * dz
            )
    }
}
