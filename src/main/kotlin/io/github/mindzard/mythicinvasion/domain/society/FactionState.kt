package io.github.mindzard.mythicinvasion.domain.society

data class FactionState(
    val factionId: String,
    val type: FactionType,
    val displayName: String,
    val population: Int = 0,
    val resources: Double = 0.0,
    val militaryStrength: Double = 0.0,
    val influence: Double = 0.0,
    val relations: Map<String, FactionRelation> = emptyMap(),
    val lastUpdatedMillis: Long = 0L
) {

    init {
        require(factionId.isNotBlank()) {
            "Faction ID cannot be blank."
        }

        require(displayName.isNotBlank()) {
            "Faction display name cannot be blank."
        }

        require(population >= 0) {
            "Faction population cannot be negative."
        }

        require(resources >= 0.0) {
            "Faction resources cannot be negative."
        }

        require(militaryStrength in 0.0..1.0) {
            "Faction military strength must be between 0.0 and 1.0."
        }

        require(influence in 0.0..1.0) {
            "Faction influence must be between 0.0 and 1.0."
        }
    }
}
