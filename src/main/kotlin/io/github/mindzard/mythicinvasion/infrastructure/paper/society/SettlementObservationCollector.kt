package io.github.mindzard.mythicinvasion.infrastructure.paper.society

import io.github.mindzard.mythicinvasion.domain.society.SettlementObservation
import org.bukkit.Location
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.entity.Entity
import org.bukkit.entity.IronGolem
import org.bukkit.entity.Villager
import kotlin.math.floor
import kotlin.math.sqrt

class SettlementObservationCollector(
    private val plugin: JavaPlugin
) {

    companion object {
        private const val GRID_SIZE = 64
        private const val MAX_CLUSTER_DISTANCE = 72.0
        private const val MIN_SETTLEMENT_POPULATION = 2
        private const val SETTLEMENT_RADIUS = 64

        private const val CLUSTER_KEY_DIVISOR = 1_000_000
    }

    fun collect(): List<SettlementObservation> {

        val nowMillis =
            System.currentTimeMillis()

        val observations =
            mutableListOf<SettlementObservation>()

        for (world in plugin.server.worlds) {

            val villagers =
                world.entities
                    .asSequence()
                    .filterIsInstance<Villager>()
                    .filter { !it.isDead }
                    .toList()

            if (
                villagers.size <
                MIN_SETTLEMENT_POPULATION
            ) {
                continue
            }

            val golems =
                world.entities
                    .asSequence()
                    .filterIsInstance<IronGolem>()
                    .filter { !it.isDead }
                    .toList()

            val clusters =
                mergeNearbyClusters(
                    clusterVillagers(villagers)
                )

            for (cluster in clusters) {

                if (
                    cluster.size <
                    MIN_SETTLEMENT_POPULATION
                ) {
                    continue
                }

                val centerX =
                    cluster
                        .map { it.location.blockX }
                        .average()
                        .toInt()

                val centerY =
                    cluster
                        .map { it.location.blockY }
                        .average()
                        .toInt()

                val centerZ =
                    cluster
                        .map { it.location.blockZ }
                        .average()
                        .toInt()

                val golemCount =
                    golems.count { golem ->

                        val location =
                            golem.location

                        distanceSquared(
                            location.blockX,
                            location.blockY,
                            location.blockZ,
                            centerX,
                            centerY,
                            centerZ
                        ) <=
                            SETTLEMENT_RADIUS *
                            SETTLEMENT_RADIUS
                    }

                val settlementId =
                    createStableSettlementId(
                        worldName = world.name,
                        centerX = centerX,
                        centerY = centerY,
                        centerZ = centerZ
                    )

                observations.add(
                    SettlementObservation(
                        settlementId =
                            settlementId,

                        name =
                            createSettlementName(
                                worldName = world.name,
                                centerX = centerX,
                                centerZ = centerZ
                            ),

                        worldName =
                            world.name,

                        centerX =
                            centerX,

                        centerY =
                            centerY,

                        centerZ =
                            centerZ,

                        radius =
                            SETTLEMENT_RADIUS,

                        villagerCount =
                            cluster.size,

                        ironGolemCount =
                            golemCount,

                        lastUpdatedMillis =
                            nowMillis
                    )
                )
            }
        }

        return observations
    }

    private fun clusterVillagers(
        villagers: List<Villager>
    ): List<List<Villager>> {

        if (villagers.isEmpty()) {
            return emptyList()
        }

        val cellMap =
            HashMap<GridCell, MutableList<Int>>()

        villagers.forEachIndexed { index, villager ->

            val location =
                villager.location

            val cell =
                GridCell(
                    x =
                        floor(
                            location.x / GRID_SIZE
                        ).toInt(),

                    z =
                        floor(
                            location.z / GRID_SIZE
                        ).toInt()
                )

            cellMap
                .getOrPut(cell) {
                    mutableListOf()
                }
                .add(index)
        }

        val parent =
            IntArray(villagers.size) {
                it
            }

        fun find(value: Int): Int {

            var current =
                value

            while (
                parent[current] != current
            ) {
                parent[current] =
                    parent[parent[current]]

                current =
                    parent[current]
            }

            return current
        }

        fun union(
            first: Int,
            second: Int
        ) {

            val rootFirst =
                find(first)

            val rootSecond =
                find(second)

            if (
                rootFirst != rootSecond
            ) {
                parent[rootSecond] =
                    rootFirst
            }
        }

        villagers.forEachIndexed {
            index,
            villager ->

            val location =
                villager.location

            val baseCell =
                GridCell(
                    x =
                        floor(
                            location.x / GRID_SIZE
                        ).toInt(),

                    z =
                        floor(
                            location.z / GRID_SIZE
                        ).toInt()
                )

            for (
                dx in -1..1
            ) {
                for (
                    dz in -1..1
                ) {

                    val cell =
                        GridCell(
                            x =
                                baseCell.x + dx,
                            z =
                                baseCell.z + dz
                        )

                    val nearby =
                        cellMap[cell]
                            ?: continue

                    for (
                        otherIndex in nearby
                    ) {

                        if (
                            otherIndex >= index
                        ) {
                            continue
                        }

                        val other =
                            villagers[otherIndex]

                        val distance =
                            distanceSquared(
                                location.blockX,
                                location.blockY,
                                location.blockZ,
                                other.location.blockX,
                                other.location.blockY,
                                other.location.blockZ
                            )

                        if (
                            distance <=
                            MAX_CLUSTER_DISTANCE *
                            MAX_CLUSTER_DISTANCE
                        ) {
                            union(
                                index,
                                otherIndex
                            )
                        }
                    }
                }
            }
        }

        val clusters =
            HashMap<Int, MutableList<Villager>>()

        villagers.forEachIndexed {
            index,
            villager ->

            val root =
                find(index)

            clusters
                .getOrPut(root) {
                    mutableListOf()
                }
                .add(villager)
        }

        return clusters.values.toList()
    }


    private fun mergeNearbyClusters(
        clusters: List<List<Villager>>
    ): List<List<Villager>> {

        if (clusters.size <= 1) {
            return clusters
        }

        val working =
            clusters
                .map { it.toMutableList() }
                .toMutableList()

        var merged = true

        while (merged) {
            merged = false

            outer@ for (firstIndex in working.indices) {
                val first = working[firstIndex]
                val firstCenter =
                    clusterCenter(first)

                for (secondIndex in firstIndex + 1 until working.size) {
                    val second =
                        working[secondIndex]

                    val secondCenter =
                        clusterCenter(second)

                    if (
                        distanceSquared(
                            firstCenter.x,
                            firstCenter.y,
                            firstCenter.z,
                            secondCenter.x,
                            secondCenter.y,
                            secondCenter.z
                        ) <=
                        MAX_CLUSTER_DISTANCE *
                        MAX_CLUSTER_DISTANCE
                    ) {
                        first.addAll(second)
                        working.removeAt(secondIndex)
                        merged = true
                        break@outer
                    }
                }
            }
        }

        return working
    }

    private fun clusterCenter(
        villagers: List<Villager>
    ): Location {
        val first =
            villagers.first().location

        return Location(
            first.world,
            villagers
                .map { it.location.x }
                .average(),
            villagers
                .map { it.location.y }
                .average(),
            villagers
                .map { it.location.z }
                .average()
        )
    }

    private fun createStableSettlementId(
        worldName: String,
        centerX: Int,
        centerY: Int,
        centerZ: Int
    ): String {

        val bucketX =
            Math.floorDiv(
                centerX,
                SETTLEMENT_RADIUS
            )

        val bucketY =
            Math.floorDiv(
                centerY,
                SETTLEMENT_RADIUS
            )

        val bucketZ =
            Math.floorDiv(
                centerZ,
                SETTLEMENT_RADIUS
            )

        return buildString {

            append("settlement_")
            append(
                worldName
                    .lowercase()
                    .replace(
                        Regex("[^a-z0-9_-]"),
                        "_"
                    )
            )

            append("_")
            append(bucketX)

            append("_")
            append(bucketY)

            append("_")
            append(bucketZ)

            append("_")
            append(
                CLUSTER_KEY_DIVISOR
            )
        }
    }

    private fun createSettlementName(
        worldName: String,
        centerX: Int,
        centerZ: Int
    ): String {

        return "Settlement ${worldName} " +
            "(${centerX}, ${centerZ})"
    }

    private fun distanceSquared(
        x1: Int,
        y1: Int,
        z1: Int,
        x2: Int,
        y2: Int,
        z2: Int
    ): Double {

        val dx =
            (x1 - x2).toDouble()

        val dy =
            (y1 - y2).toDouble()

        val dz =
            (z1 - z2).toDouble()

        return (
            dx * dx +
                dy * dy +
                dz * dz
            )
    }

    private data class GridCell(
        val x: Int,
        val z: Int
    )
}
