package io.github.mindzard.mythicinvasion.domain.society

import java.util.UUID

data class VillagerIdentitySnapshot(
    val villagerId: UUID,
    val settlementId: String,
    val worldName: String,
    val x: Int,
    val y: Int,
    val z: Int,
    val profession: String,
    val villagerType: String,
    val villagerLevel: Int,
    val villagerExperience: Int,
    val isAdult: Boolean,
    val knownPlayerCount: Int,
    val timestampMillis: Long
)
