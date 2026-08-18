package io.github.mindzard.mythicinvasion.application.behaviour

import kotlin.math.pow

/**
 * Calculates how much influence an observation should retain over time.
 *
 * We use exponential decay based on a configurable half-life.
 *
 * Example:
 * half-life = 30 minutes
 *
 * 0 minutes old  -> 1.000 influence
 * 30 minutes old -> 0.500 influence
 * 60 minutes old -> 0.250 influence
 * 90 minutes old -> 0.125 influence
 *
 * This keeps recent behaviour important while preventing ancient
 * behaviour from dominating the current player profile.
 */
class BehaviourDecayEngine(
    halfLifeMillis: Long
) {

    private val halfLifeMillis =
        halfLifeMillis.coerceAtLeast(1L)

    fun decayFactor(
        elapsedMillis: Long
    ): Double {

        val elapsed =
            elapsedMillis.coerceAtLeast(0L)

        if (elapsed == 0L) {
            return 1.0
        }

        val elapsedRatio =
            elapsed.toDouble() /
                halfLifeMillis.toDouble()

        return 0.5.pow(elapsedRatio)
    }

    fun decay(
        value: Double,
        elapsedMillis: Long
    ): Double {

        if (value <= 0.0) {
            return 0.0
        }

        return value *
            decayFactor(elapsedMillis)
    }
}
