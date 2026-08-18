package io.github.mindzard.mythicinvasion.application.intelligence

import io.github.mindzard.mythicinvasion.domain.behaviour.BehaviourFeatures
import io.github.mindzard.mythicinvasion.domain.behaviour.PlayerBehaviourProfile
import io.github.mindzard.mythicinvasion.domain.intelligence.BehaviourArchetype
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class BehaviourIntelligenceEngineTest {

    private val engine =
        BehaviourIntelligenceEngine()

    @Test
    fun minerHeavyProfileShouldIdentifyMinerAsDominant() {

        val playerId =
            UUID.randomUUID()

        val profile =
            PlayerBehaviourProfile(
                playerId = playerId,
                totalEvents = 1_000L,
                features = BehaviourFeatures(
                    miningTendency = 1.0,
                    buildingTendency = 0.1,
                    combatTendency = 0.1,
                    movementTendency = 0.3,
                    activityLevel = 0.8
                )
            )

        val result =
            engine.calculate(
                profile = profile,
                previous = null
            )

        assertEquals(
            BehaviourArchetype.MINER,
            result.dominantArchetype
        )

        assertTrue(
            result.score(
                BehaviourArchetype.MINER
            ) >
                result.score(
                    BehaviourArchetype.BUILDER
                )
        )
    }

    @Test
    fun confidenceShouldIncreaseWithMoreObservations() {

        val playerId =
            UUID.randomUUID()

        val lowEvidence =
            PlayerBehaviourProfile(
                playerId = playerId,
                totalEvents = 10L
            )

        val highEvidence =
            PlayerBehaviourProfile(
                playerId = playerId,
                totalEvents = 1_000L
            )

        val lowResult =
            engine.calculate(
                profile = lowEvidence,
                previous = null
            )

        val highResult =
            engine.calculate(
                profile = highEvidence,
                previous = null
            )

        assertTrue(
            highResult.confidence >
                lowResult.confidence
        )
    }

    @Test
    fun scoresShouldRemainInsideNormalizedRange() {

        val playerId =
            UUID.randomUUID()

        val profile =
            PlayerBehaviourProfile(
                playerId = playerId,
                totalEvents = 10_000L,
                features = BehaviourFeatures(
                    miningTendency = 1.0,
                    buildingTendency = 1.0,
                    combatTendency = 1.0,
                    movementTendency = 1.0,
                    activityLevel = 1.0
                )
            )

        val result =
            engine.calculate(
                profile = profile,
                previous = null
            )

        result.archetypeScores.values.forEach { score ->

            assertTrue(
                score >= 0.0
            )

            assertTrue(
                score <= 1.0
            )
        }

        assertTrue(
            result.confidence >= 0.0
        )

        assertTrue(
            result.confidence <= 1.0
        )
    }
}
