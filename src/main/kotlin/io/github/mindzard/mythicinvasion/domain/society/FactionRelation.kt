package io.github.mindzard.mythicinvasion.domain.society

data class FactionRelation(
    val sourceFactionId: String,
    val targetFactionId: String,
    val value: Double = 0.0,
    val trust: Double = 0.0,
    val fear: Double = 0.0,
    val respect: Double = 0.0,
    val lastUpdatedMillis: Long = 0L
) {

    init {
        require(value in -1.0..1.0) {
            "Faction relation value must be between -1.0 and 1.0."
        }

        require(trust in 0.0..1.0) {
            "Faction trust must be between 0.0 and 1.0."
        }

        require(fear in 0.0..1.0) {
            "Faction fear must be between 0.0 and 1.0."
        }

        require(respect in 0.0..1.0) {
            "Faction respect must be between 0.0 and 1.0."
        }
    }
}
