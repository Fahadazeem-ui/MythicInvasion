package io.github.mindzard.mythicinvasion.application.ai

import io.github.mindzard.mythicinvasion.application.intelligence.BehaviourIntelligenceStore
import io.github.mindzard.mythicinvasion.application.society.SettlementSocialStore
import io.github.mindzard.mythicinvasion.application.society.SocietyStateStore
import io.github.mindzard.mythicinvasion.application.world.WorldStateStore
import io.github.mindzard.mythicinvasion.domain.ai.AiPlayerContext
import io.github.mindzard.mythicinvasion.domain.ai.AiSettlementContext
import io.github.mindzard.mythicinvasion.domain.ai.AiStrategicContext

class AiContextAssembler(
    private val worldStateStore: WorldStateStore,
    private val behaviourIntelligenceStore: BehaviourIntelligenceStore,
    private val societyStateStore: SocietyStateStore,
    private val settlementSocialStore: SettlementSocialStore
) {

    fun assemble(): AiStrategicContext {

        val intelligenceProfiles =
            behaviourIntelligenceStore
                .snapshot()

        val socialProfiles =
            settlementSocialStore
                .snapshot()
                .associateBy {
                    it.settlementId
                }

        val settlements =
            societyStateStore
                .current()
                .settlements
                .values
                .map { settlement ->

                    val social =
                        socialProfiles[
                            settlement.settlementId
                        ]

                    AiSettlementContext(
                        settlementId =
                            settlement.settlementId,

                        population =
                            settlement.population,

                        safety =
                            settlement.safetyLevel,

                        prosperity =
                            settlement.prosperityLevel,

                        trustedPlayers =
                            social
                                ?.trustedPlayers
                                ?.size
                                ?: 0,

                        neutralPlayers =
                            social
                                ?.neutralPlayers
                                ?.size
                                ?: 0,

                        hostilePlayers =
                            social
                                ?.hostilePlayers
                                ?.size
                                ?: 0,

                        averageTrust =
                            social
                                ?.averageTrust
                                ?: 0.0,

                        averageThreat =
                            social
                                ?.averageThreat
                                ?: 0.0
                    )
                }

        val players =
            intelligenceProfiles
                .map { profile ->

                    val features =
                        profile

                            .let {
                                AiPlayerContext(
                                    playerName =
                                        "player-${it.playerId}",

                                    archetype =
                                        it.dominantArchetype,

                                    confidence =
                                        it.confidence,

                                    mining =
                                        0.0,

                                    building =
                                        0.0,

                                    combat =
                                        0.0,

                                    movement =
                                        0.0,

                                    activity =
                                        0.0
                                )
                            }

                    features
                }

        return AiStrategicContext(
            world =
                worldStateStore.current(),

            players =
                players,

            settlements =
                settlements,

            generatedAtMillis =
                System.currentTimeMillis()
        )
    }
}
