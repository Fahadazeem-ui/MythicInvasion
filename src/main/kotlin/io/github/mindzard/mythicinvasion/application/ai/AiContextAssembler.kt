package io.github.mindzard.mythicinvasion.application.ai

import io.github.mindzard.mythicinvasion.application.intelligence.BehaviourIntelligenceStore
import io.github.mindzard.mythicinvasion.application.society.SettlementSocialStore
import io.github.mindzard.mythicinvasion.application.society.SocietyStateStore
import io.github.mindzard.mythicinvasion.application.world.WorldStateStore
import io.github.mindzard.mythicinvasion.domain.ai.AiPlayerContext
import io.github.mindzard.mythicinvasion.domain.ai.AiSettlementContext
import io.github.mindzard.mythicinvasion.domain.ai.AiStrategicContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AiContextAssembler(
    private val worldStateStore: WorldStateStore,
    private val behaviourIntelligenceStore: BehaviourIntelligenceStore,
    private val societyStateStore: SocietyStateStore,
    private val settlementSocialStore: SettlementSocialStore
) {

    private val json = Json {
        encodeDefaults = true
    }

    fun assemble(): AiStrategicContext {

        val behaviourProfiles =
            behaviourIntelligenceStore.snapshot()

        val socialProfiles =
            settlementSocialStore
                .snapshot()
                .associateBy {
                    it.settlementId
                }

        val settlementStates =
            societyStateStore
                .current()
                .settlements
                .values

        val players =
            behaviourProfiles.map { profile ->

                AiPlayerContext(
                    playerName =
                        "player-${profile.playerId}",

                    archetype =
                        profile.dominantArchetype,

                    confidence =
                        profile.confidence,

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

        val settlements =
            settlementStates.map { settlement ->

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

    fun toJson(
        context: AiStrategicContext,
        maximumCharacters: Int
    ): String {

        val world =
            buildJsonObject {

                put(
                    "totalPlayers",
                    context.world.totalPlayers
                )

                put(
                    "totalVillagers",
                    context.world.totalVillagers
                )

                put(
                    "totalPillagers",
                    context.world.totalPillagers
                )

                put(
                    "totalHostileMobs",
                    context.world.totalHostileMobs
                )

                put(
                    "totalPassiveAnimals",
                    context.world.totalPassiveAnimals
                )

                put(
                    "totalWorlds",
                    context.world.totalWorlds
                )

                put(
                    "globalActivityLevel",
                    context.world.globalActivityLevel
                )

                put(
                    "globalThreatLevel",
                    context.world.globalThreatLevel
                )

                put(
                    "lastUpdatedMillis",
                    context.world.lastUpdatedMillis
                )
            }

        val players =
            buildJsonArray {

                context.players.forEach { player ->

                    add(
                        buildJsonObject {

                            put(
                                "player",
                                player.playerName
                            )

                            put(
                                "archetype",
                                player.archetype
                                    ?.name
                                    ?: "UNKNOWN"
                            )

                            put(
                                "confidence",
                                player.confidence
                            )

                            put(
                                "mining",
                                player.mining
                            )

                            put(
                                "building",
                                player.building
                            )

                            put(
                                "combat",
                                player.combat
                            )

                            put(
                                "movement",
                                player.movement
                            )

                            put(
                                "activity",
                                player.activity
                            )
                        }
                    )
                }
            }

        val settlements =
            buildJsonArray {

                context.settlements.forEach { settlement ->

                    add(
                        buildJsonObject {

                            put(
                                "settlementId",
                                settlement.settlementId
                            )

                            put(
                                "population",
                                settlement.population
                            )

                            put(
                                "safety",
                                settlement.safety
                            )

                            put(
                                "prosperity",
                                settlement.prosperity
                            )

                            put(
                                "trustedPlayers",
                                settlement.trustedPlayers
                            )

                            put(
                                "neutralPlayers",
                                settlement.neutralPlayers
                            )

                            put(
                                "hostilePlayers",
                                settlement.hostilePlayers
                            )

                            put(
                                "averageTrust",
                                settlement.averageTrust
                            )

                            put(
                                "averageThreat",
                                settlement.averageThreat
                            )
                        }
                    )
                }
            }

        val root =
            buildJsonObject {

                put(
                    "generatedAtMillis",
                    context.generatedAtMillis
                )

                put(
                    "world",
                    world
                )

                put(
                    "players",
                    players
                )

                put(
                    "settlements",
                    settlements
                )
            }

        return json
            .encodeToString(
                kotlinx.serialization.json.JsonObject.serializer(),
                root
            )
            .take(
                maximumCharacters
            )
    }
}
