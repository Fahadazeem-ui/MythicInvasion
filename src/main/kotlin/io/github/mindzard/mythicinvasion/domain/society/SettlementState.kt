package io.github.mindzard.mythicinvasion.domain.society

data class SettlementState(
    val settlementId: String,
    val name: String,
    val worldName: String,
    val centerX: Int,
    val centerY: Int,
    val centerZ: Int,
    val radius: Int = 32,
    val population: Int = 0,
    val foodLevel: Double = 1.0,
    val safetyLevel: Double = 1.0,
    val prosperityLevel: Double = 0.0,
    val ownerFactionId: String? = null,
    val lastUpdatedMillis: Long = 0L
) {

    init {
        require(settlementId.isNotBlank()) {
            "Settlement ID cannot be blank."
        }

        require(name.isNotBlank()) {
            "Settlement name cannot be blank."
        }

        require(worldName.isNotBlank()) {
            "Settlement world name cannot be blank."
        }

        require(radius > 0) {
            "Settlement radius must be greater than zero."
        }

        require(population >= 0) {
            "Settlement population cannot be negative."
        }

        require(foodLevel in 0.0..1.0) {
            "Settlement food level must be between 0.0 and 1.0."
        }

        require(safetyLevel in 0.0..1.0) {
            "Settlement safety level must be between 0.0 and 1.0."
        }

        require(prosperityLevel in 0.0..1.0) {
            "Settlement prosperity level must be between 0.0 and 1.0."
        }
    }
}
