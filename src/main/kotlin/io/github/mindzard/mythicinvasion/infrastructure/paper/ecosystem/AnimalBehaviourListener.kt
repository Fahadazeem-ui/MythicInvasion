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
import kotlin.random.Random

class AnimalBehaviourListener(
    private val plugin: JavaPlugin
) : Listener {

    companion object {
        private const val THINK_INTERVAL_TICKS = 60L

        private const val DANGER_RADIUS = 14.0
        private const val HERD_RADIUS = 14.0

        private const val MIN_HERD_SPACING = 3.5
        private const val COMFORT_RADIUS = 10.0
        private const val RETURN_RADIUS = 18.0

        private const val FLEE_DISTANCE = 9.0
        private const val HERD_RETURN_SPEED = 0.60
        private const val FLEE_SPEED = 1.00
        private const val OWNER_SPEED = 0.65

        private const val ACTION_COOLDOWN_MILLIS = 7_000L
        private const val WANDER_MIN_DELAY_MILLIS = 8_000L
        private const val WANDER_MAX_DELAY_MILLIS = 18_000L

        private const val MEMORY_HALF_LIFE_MILLIS = 120_000L
        private const val MAX_DANGER_MEMORY = 100.0

        private const val PLAYER_ATTACK_MEMORY_GAIN = 30.0
        private const val MONSTER_ATTACK_MEMORY_GAIN = 35.0
    }

    private data class DangerMemory(
        var score: Double,
        var updatedAtMillis: Long
    )

    private data class WanderState(
        var nextWanderAtMillis: Long
    )

    private val dangerMemory =
        ConcurrentHashMap<UUID, DangerMemory>()

    private val actionCooldowns =
        ConcurrentHashMap<UUID, Long>()

    private val wanderStates =
        ConcurrentHashMap<UUID, WanderState>()

    private val thinkTask: BukkitTask =
        plugin.server.scheduler.runTaskTimer(
            plugin,
            Runnable { think() },
            THINK_INTERVAL_TICKS,
            THINK_INTERVAL_TICKS
        )

    private fun think() {
        val now = System.currentTimeMillis()

        for (world in plugin.server.worlds) {
            val animals =
                world.entities
                    .asSequence()
                    .filterIsInstance<Animals>()
                    .filter { !it.isDead && it.isValid }
                    .toList()

            for (animal in animals) {
                if (isOnCooldown(animal, now)) {
                    continue
                }

                val danger = findNearestDanger(animal)

                if (danger != null) {
                    rememberDanger(danger)
                    flee(animal, danger)
                    setCooldown(animal, now)
                    continue
                }

                if (animal is Wolf && animal.isTamed) {
                    val owner = animal.owner as? Player

                    if (owner != null && isValidPlayer(owner)) {
                        approachOwner(animal, owner)
                        setCooldown(animal, now)
                        continue
                    }
                }

                val herdMate = findNearestHerdMate(animal)
                val herdDistance =
                    herdMate?.let {
                        sqrt(
                            distanceSquared(
                                animal,
                                it
                            )
                        )
                    } ?: Double.MAX_VALUE

                when {
                    herdDistance < MIN_HERD_SPACING -> {
                        moveAwayFrom(
                            animal,
                            herdMate,
                            MIN_HERD_SPACING + 1.5
                        )
                        setCooldown(animal, now)
                    }

                    herdDistance > RETURN_RADIUS -> {
                        moveTowardHerd(
                            animal,
                            herdMate
                        )
                        setCooldown(animal, now)
                    }

                    herdDistance <= COMFORT_RADIUS -> {
                        maybeWander(animal, now)
                    }

                    else -> {
                        moveTowardHerd(
                            animal,
                            herdMate
                        )
                        setCooldown(animal, now)
                    }
                }
            }
        }

        decayAllMemory(now)
    }

    @EventHandler(
        priority = EventPriority.MONITOR,
        ignoreCancelled = true
    )
    fun onAnimalDamaged(
        event: EntityDamageByEntityEvent
    ) {
        val animal = event.entity as? Animals
            ?: return

        if (animal.isDead) {
            return
        }

        when (val attacker = event.damager) {
            is Player ->
                addDangerMemory(
                    attacker.uniqueId,
                    PLAYER_ATTACK_MEMORY_GAIN
                )

            is Monster ->
                addDangerMemory(
                    attacker.uniqueId,
                    MONSTER_ATTACK_MEMORY_GAIN
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
        event.entity as? Animals
            ?: return

        val target = event.target
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
        val animal = event.rightClicked as? Animals
            ?: return

        val player = event.player

        if (!isValidPlayer(player)) {
            return
        }

        dangerMemory.remove(player.uniqueId)

        if (
            animal is Wolf &&
            animal.owner?.uniqueId == player.uniqueId
        ) {
            animal.lookAt(player)
        }
    }

    @EventHandler
    fun onPlayerQuit(
        event: PlayerQuitEvent
    ) {
        dangerMemory.remove(event.player.uniqueId)
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
            .filter { it.uniqueId != animal.uniqueId }
            .filter { !it.isDead }
            .filter { isDangerous(it) }
            .minByOrNull {
                distanceSquared(animal, it)
            }
    }

    private fun isDangerous(
        entity: LivingEntity
    ): Boolean {
        if (entity is Monster) {
            return true
        }

        if (entity is Player) {
            val memory =
                dangerMemory[entity.uniqueId]
                    ?: return false

            decayMemory(
                memory,
                System.currentTimeMillis()
            )

            return memory.score >= 15.0
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
            .filter { it.uniqueId != animal.uniqueId }
            .filter { !it.isDead }
            .filter { it.type == animal.type }
            .minByOrNull {
                distanceSquared(animal, it)
            }
    }

    private fun flee(
        animal: Animals,
        danger: LivingEntity
    ) {
        val mob = animal as? Mob
            ?: return

        mob.pathfinder.moveTo(
            calculateFleeLocation(
                animal.location,
                danger.location
            ),
            FLEE_SPEED
        )
    }

    private fun moveAwayFrom(
        animal: Animals,
        herdMate: Animals?,
        distance: Double
    ) {
        val mate = herdMate ?: return
        val mob = animal as? Mob
            ?: return

        mob.pathfinder.moveTo(
            calculateFleeLocation(
                animal.location,
                mate.location,
                distance
            ),
            HERD_RETURN_SPEED
        )
    }

    private fun moveTowardHerd(
        animal: Animals,
        herdMate: Animals?
    ) {
        val mate = herdMate ?: return
        val mob = animal as? Mob
            ?: return

        val destination =
            calculateComfortLocation(
                animal.location,
                mate.location
            )

        mob.pathfinder.moveTo(
            destination,
            HERD_RETURN_SPEED
        )
    }

    private fun maybeWander(
        animal: Animals,
        now: Long
    ) {
        val state =
            wanderStates.computeIfAbsent(
                animal.uniqueId
            ) {
                WanderState(
                    now +
                        Random.nextLong(
                            WANDER_MIN_DELAY_MILLIS,
                            WANDER_MAX_DELAY_MILLIS
                        )
                )
            }

        if (now < state.nextWanderAtMillis) {
            return
        }

        val mob = animal as? Mob
            ?: return

        mob.pathfinder.moveTo(
            randomNearbyLocation(
                animal.location
            ),
            0.50
        )

        state.nextWanderAtMillis =
            now +
                Random.nextLong(
                    WANDER_MIN_DELAY_MILLIS,
                    WANDER_MAX_DELAY_MILLIS
                )
    }

    private fun approachOwner(
        animal: Wolf,
        owner: Player
    ) {
        if (
            animal.location.distanceSquared(
                owner.location
            ) <= 9.0
        ) {
            animal.lookAt(owner)
            return
        }

        animal.pathfinder.moveTo(
            owner.location,
            OWNER_SPEED
        )
    }

    private fun calculateFleeLocation(
        animal: Location,
        danger: Location,
        distance: Double = FLEE_DISTANCE
    ): Location {
        var dx = animal.x - danger.x
        var dz = animal.z - danger.z

        val length =
            sqrt(
                dx * dx +
                    dz * dz
            )

        if (length <= 0.001) {
            dx = 1.0
            dz = 0.0
        } else {
            dx /= length
            dz /= length
        }

        return animal.clone().apply {
            x += dx * distance
            z += dz * distance
        }
    }

    private fun calculateComfortLocation(
        animal: Location,
        herdMate: Location
    ): Location {
        var dx = herdMate.x - animal.x
        var dz = herdMate.z - animal.z

        val length =
            sqrt(
                dx * dx +
                    dz * dz
            )

        if (length <= 0.001) {
            return animal
        }

        val desired =
            COMFORT_RADIUS.coerceAtMost(
                length
            )

        val scale =
            (
                (length - desired) /
                    length
                )
                .coerceIn(
                    0.0,
                    0.40
                )

        return animal.clone().apply {
            x += dx * scale
            z += dz * scale
        }
    }

    private fun randomNearbyLocation(
        origin: Location
    ): Location {
        val angle =
            Random.nextDouble(
                0.0,
                Math.PI * 2.0
            )

        val distance =
            Random.nextDouble(
                5.0,
                11.0
            )

        return origin.clone().apply {
            x +=
                kotlin.math.cos(angle) *
                    distance
            z +=
                kotlin.math.sin(angle) *
                    distance
        }
    }

    private fun rememberDanger(
        danger: LivingEntity
    ) {
        val gain =
            if (danger is Monster) {
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

        dangerMemory.compute(id) { _, existing ->
            val memory =
                existing
                    ?: DangerMemory(
                        0.0,
                        now
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

            memory.updatedAtMillis = now
            memory
        }
    }

    private fun decayAllMemory(
        now: Long
    ) {
        dangerMemory.values.forEach {
            decayMemory(it, now)
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

        if (elapsed <= 0L) {
            return
        }

        val halfLives =
            elapsed.toDouble() /
                MEMORY_HALF_LIFE_MILLIS.toDouble()

        memory.score *=
            Math.pow(
                0.5,
                halfLives
            )

        memory.updatedAtMillis = now
    }

    private fun isOnCooldown(
        animal: Animals,
        now: Long
    ): Boolean {
        val next =
            actionCooldowns[animal.uniqueId]
                ?: return false

        return now < next
    }

    private fun setCooldown(
        animal: Animals,
        now: Long
    ) {
        actionCooldowns[animal.uniqueId] =
            now +
                ACTION_COOLDOWN_MILLIS
    }

    private fun isValidPlayer(
        player: Player
    ): Boolean {
        return player.isOnline &&
            !player.isDead &&
            player.gameMode != GameMode.SPECTATOR
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
