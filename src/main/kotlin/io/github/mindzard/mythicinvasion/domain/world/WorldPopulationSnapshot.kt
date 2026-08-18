package io.github.mindzard.mythicinvasion.domain.world

data class WorldPopulationSnapshot(
    val worldName: String,
    val playerCount: Int,
    val villagerCount: Int,
    val pillagerCount: Int,
    val hostileMobCount: Int,
    val passiveAnimalCount: Int,
    val ironGolemCount: Int,
    val timestampMillis: Long
)
