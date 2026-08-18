package io.github.mindzard.mythicinvasion.domain.world

data class WorldSnapshot(
    val timestampMillis: Long,
    val dayTime: Long,
    val isDay: Boolean,
    val isRaining: Boolean,
    val populations: List<WorldPopulationSnapshot>
) {

    val totalPlayers: Int
        get() = populations.sumOf { it.playerCount }

    val totalVillagers: Int
        get() = populations.sumOf { it.villagerCount }

    val totalPillagers: Int
        get() = populations.sumOf { it.pillagerCount }

    val totalHostileMobs: Int
        get() = populations.sumOf { it.hostileMobCount }

    val totalPassiveAnimals: Int
        get() = populations.sumOf { it.passiveAnimalCount }
}
