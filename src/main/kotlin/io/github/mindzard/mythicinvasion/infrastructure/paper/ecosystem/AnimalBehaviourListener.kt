package io.github.mindzard.mythicinvasion.infrastructure.paper.ecosystem

import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Animals
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mob
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.entity.Wolf
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerQuitEvent
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

        private const val HERD_RADIUS =
            10.0

        private const val FLEE_DISTANCE =
            7.0

        private const val HERD_DISTANCE =
            6.0

        private const val FLEE_SPEED =
            0.95

        private const val HERD_SPEED =
            0.55

        private const val WOLF_OWNER_SPEED =
            0.65

        private const val ACTION_COOLDOWN_MILLIS =
            4_000L

        private const val MEMORY_HALF_LIFE_MILLIS =
            90_000L

        private const val MAX_DANGER_MEMORY =
            100.0

        private const val PLAYER_ATTACK_MEMORY_GAIN =
            25.0

        private const val MONSTER_ATTACK_MEMORY_GAIN =
            30.0
    }

    private data class DangerMemory(
        var score: Double,
        var updatedAtMillis: Long
    )

    private val dangerMemory =
        ConcurrentHashMap<UUID, DangerMemory>()

    private val actionCooldowns =
        ConcurrentHashMap<UUID, Long>()

    private val thinkTask: BukkitTask =
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

                /*
                 * Wolves keep their native ownership/taming
                 * system. We only give an owner-awareness signal.
                 */
                if (
                    animal is Wolf &&
                    animal.isTamed
                ) {

                    val owner =
                        animal.owner as? Player

                    if (
                        owner != null &&
                        isValidPlayer(
                            owner
                        )
                    ) {

                        approachOwner(
                            animal,
                            owner
                        )

                        setCooldown(
                            animal,
                            now
                        )

                        continue
                    }
                }

                val herdMate =
                    findNearestHerdMate(
                        animal
                    )

                if (herdMate != null) {

                    moveTowardHerd(
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

        decayAllMemory(
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

        when (
            val attacker =
                event.damager
        ) {

            is Player -> {

                addDangerMemory(
                    attacker.uniqueId,
                    PLAYER_ATTACK_MEMORY_GAIN
                )
            }

            is Monster -> {

                addDangerMemory(
                    attacker.uniqueId,
                    MONSTER_ATTACK_MEMORY_GAIN
                )
            }
        }
    }

    @EventHandler(
        priority = EventPriority.HIGHEST,
        ignoreCancelled = true
    )
    fun onAnimalTarget(
        event: EntityTargetLivingEntityEvent
    ) {

        event.entity as? Animals
            ?: return

        val target =
            event.target
                ?: return

        if (
            target is Player &&
            !isValidPlayer(
                target
            )
        ) {

            event.target =
                null
        }

        /*
         * Do not replace normal Minecraft target selection.
         * This listener only removes clearly invalid player
         * targets and supplies our own reactions in think().
         */
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
         * A direct interaction is treated as a positive
         * signal and removes any temporary danger memory
         * associated with that player.
         */
        dangerMemory.remove(
            player.uniqueId
        )

        if (
            animal is Wolf &&
            animal.owner?.uniqueId ==
                player.uniqueId
        ) {

            animal.lookAt(
                player
            )
        }
    }

    @EventHandler
    fun onPlayerQuit(
        event: PlayerQuitEvent
    ) {

        dangerMemory.remove(
            event.player.uniqueId
        )
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
                it.uniqueId !=
                    animal.uniqueId
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

            val memory =
                dangerMemory[
                    entity.uniqueId
                ]
                    ?: return false

            decayMemory(
                memory,
                System.currentTimeMillis()
            )

            return memory.score >=
                15.0
        }

        return false
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

        val mob =
            animal as? Mob
                ?: return

        val destination =
            calculateFleeLocation(
                animal.location,
                danger.location
            )

        mob.pathfinder.moveTo(
            destination,
            FLEE_SPEED
        )

        mob.lookAt(
            danger
        )
    }

    private fun approachOwner(
        animal: Wolf,
        owner: Player
    ) {

        if (
            animal.location.distanceSquared(
                owner.location
            ) <=
            9.0
        ) {

            animal.lookAt(
                owner
            )

            return
        }

        animal.pathfinder.moveTo(
            owner.location,
            WOLF_OWNER_SPEED
        )

        animal.lookAt(
            owner
        )
    }

    private fun moveTowardHerd(
        animal: Animals,
        herdMate: Animals
    ) {

        val mob =
            animal as? Mob
                ?: return

        val destination =
            calculateHerdLocation(
                animal.location,
                herdMate.location
            )

        mob.pathfinder.moveTo(
            destination,
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

            dx =
                1.0

            dz =
                0.0

        } else {

            dx /=
                length

            dz /=
                length
        }

        return animal.clone().apply {

            x +=
                dx *
                    FLEE_DISTANCE

            z +=
                dz *
                    FLEE_DISTANCE
        }
    }

    private fun calculateHerdLocation(
        animal: Location,
        herdMate: Location
    ): Location {

        val dx =
            herdMate.x -
                animal.x

        val dz =
            herdMate.z -
                animal.z

        val distance =
            sqrt(
                dx * dx +
                    dz * dz
            )

        if (
            distance <=
                0.001
        ) {
            return animal
        }

        val scale =
            HERD_DISTANCE /
                distance

        return animal.clone().apply {

            x +=
                dx *
                    scale

            z +=
                dz *
                    scale
        }
    }

    private fun rememberDanger(
        danger: LivingEntity
    ) {

        val gain =
            if (
                danger is Monster
            ) {
                MONSTER_ATTACK_MEMORY_GAIN
            } else {
                PLAYER_ATTACK_MEMORY_GAIN
            }

        addDangerMemory(
            danger.uniqueId,
            gain
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

    private fun decayAllMemory(
        now: Long
    ) {

        dangerMemory.values.forEach { memory ->

            decayMemory(
                memory,
                now
            )
        }

        dangerMemory.entries.removeIf {
            it.value.score <
                0.5
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
                    .toDouble()

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

        val nextAllowed =
            actionCooldowns[
                animal.uniqueId
            ]
                ?: return false

        return now <
            nextAllowed
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
