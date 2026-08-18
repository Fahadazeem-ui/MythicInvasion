package io.github.mindzard.mythicinvasion.domain.society

data class PillagerStrategyState(
    val factionId: String = "pillagers",
    val selectedSettlementId: String? = null,
    val selectedSettlementName: String? = null,
    val targetScore: Double = 0.0,
    val targetReason: String = "No viable settlement target.",
    val assignedPillagerCount: Int = 0,
    val scoutingPillagerCount: Int = 0,
    val updatedAtMillis: Long = 0L
) {

    init {
        require(targetScore in 0.0..1.0) {
            "Pillager target score must be between 0.0 and 1.0."
        }

        require(assignedPillagerCount >= 0) {
            "Assigned pillager count cannot be negative."
        }

        require(scoutingPillagerCount >= 0) {
            "Scouting pillager count cannot be negative."
        }
    }
}
