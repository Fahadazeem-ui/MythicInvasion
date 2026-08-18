package io.github.mindzard.mythicinvasion.domain.ai

import io.github.mindzard.mythicinvasion.domain.intelligence.BehaviourArchetype
import io.github.mindzard.mythicinvasion.domain.world.WorldIntelligenceState

data class AiPlayerContext(
    val playerName: String,
    val archetype: BehaviourArchetype?,
    val confidence: Double,
    val mining: Double,
    val building: Double,
    val combat: Double,
    val movement: Double,
    val activity: Double
)

data class AiSettlementContext(
    val settlementId: String,
    val population: Int,
    val safety: Double,
    val prosperity: Double,
    val trustedPlayers: Int,
    val neutralPlayers: Int,
    val hostilePlayers: Int,
    val averageTrust: Double,
    val averageThreat: Double
)

data class AiStrategicContext(
    val world: WorldIntelligenceState,
    val players: List<AiPlayerContext>,
    val settlements: List<AiSettlementContext>,
    val generatedAtMillis: Long
)
