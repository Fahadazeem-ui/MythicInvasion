package io.github.mindzard.mythicinvasion.domain.ai

data class AiDecision(
    val strategyId: String,
    val priority: Int,
    val summary: String,
    val reasoning: String,
    val confidence: Double,
    val suggestedActions: List<String>,
    val generatedAtMillis: Long
) {

    init {
        require(strategyId.isNotBlank()) {
            "AI strategy ID cannot be blank."
        }

        require(priority in 0..100) {
            "AI strategy priority must be between 0 and 100."
        }

        require(confidence in 0.0..1.0) {
            "AI decision confidence must be between 0.0 and 1.0."
        }
    }
}
