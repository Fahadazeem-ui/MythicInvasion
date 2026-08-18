package io.github.mindzard.mythicinvasion.application.society

import io.github.mindzard.mythicinvasion.domain.society.PlayerSocialStanding
import io.github.mindzard.mythicinvasion.domain.society.PlayerVillagerRelationship
import io.github.mindzard.mythicinvasion.domain.society.SettlementSocialProfile
import io.github.mindzard.mythicinvasion.domain.society.SocialStanding
import java.util.UUID

class SettlementSocialEngine {

    fun buildProfiles(
        relationships: Collection<PlayerVillagerRelationship>
    ): List<SettlementSocialProfile> {

        return relationships
            .groupBy { it.settlementId }
            .map { (settlementId, settlementRelationships) ->

                val standings =
                    settlementRelationships
                        .groupBy { it.playerId }
                        .map { (playerId, playerRelationships) ->

                            aggregatePlayerStanding(
                                settlementId =
                                    settlementId,
                                playerId =
                                    playerId,
                                relationships =
                                    playerRelationships
                            )
                        }

                val trusted =
                    standings
                        .filter {
                            it.standing ==
                                SocialStanding.TRUSTED
                        }
                        .associate {
                            it.playerId to it.trust
                        }

                val neutral =
                    standings
                        .filter {
                            it.standing ==
                                SocialStanding.NEUTRAL
                        }
                        .associate {
                            it.playerId to it.trust
                        }

                val hostile =
                    standings
                        .filter {
                            it.standing ==
                                SocialStanding.HOSTILE ||
                                it.standing ==
                                SocialStanding.DISTRUSTED
                        }
                        .associate {
                            it.playerId to it.threat
                        }

                SettlementSocialProfile(
                    settlementId =
                        settlementId,

                    trustedPlayers =
                        trusted,

                    neutralPlayers =
                        neutral,

                    hostilePlayers =
                        hostile,

                    averageTrust =
                        if (standings.isEmpty()) {
                            0.0
                        } else {
                            standings
                                .map { it.trust }
                                .average()
                                .coerceIn(
                                    0.0,
                                    1.0
                                )
                        },

                    averageThreat =
                        if (standings.isEmpty()) {
                            0.0
                        } else {
                            standings
                                .map { it.threat }
                                .average()
                                .coerceIn(
                                    0.0,
                                    1.0
                                )
                        },

                    updatedAtMillis =
                        settlementRelationships
                            .maxOfOrNull {
                                it.updatedAtMillis
                            }
                            ?: System.currentTimeMillis()
                )
            }
    }

    fun findPlayerStanding(
        settlementId: String,
        playerId: UUID,
        relationships: Collection<PlayerVillagerRelationship>
    ): PlayerSocialStanding? {

        val matching =
            relationships.filter {
                it.settlementId ==
                    settlementId &&
                    it.playerId ==
                    playerId
            }

        if (matching.isEmpty()) {
            return null
        }

        return aggregatePlayerStanding(
            settlementId =
                settlementId,

            playerId =
                playerId,

            relationships =
                matching
        )
    }

    private fun aggregatePlayerStanding(
        settlementId: String,
        playerId: UUID,
        relationships:
            Collection<PlayerVillagerRelationship>
    ): PlayerSocialStanding {

        val averageTrust =
            relationships
                .map { it.trust }
                .average()
                .coerceIn(
                    0.0,
                    1.0
                )

        val averageThreat =
            relationships
                .map { it.threat }
                .average()
                .coerceIn(
                    0.0,
                    1.0
                )

        val standing =
            determineStanding(
                trust =
                    averageTrust,

                threat =
                    averageThreat
            )

        return PlayerSocialStanding(
            settlementId =
                settlementId,

            playerId =
                playerId,

            trust =
                averageTrust,

            threat =
                averageThreat,

            standing =
                standing,

            updatedAtMillis =
                relationships
                    .maxOfOrNull {
                        it.updatedAtMillis
                    }
                    ?: System.currentTimeMillis()
        )
    }

    private fun determineStanding(
        trust: Double,
        threat: Double
    ): SocialStanding {

        return when {
            threat >= 0.70 -> {
                SocialStanding.HOSTILE
            }

            threat >= 0.35 -> {
                SocialStanding.DISTRUSTED
            }

            trust >= 0.60 -> {
                SocialStanding.TRUSTED
            }

            else -> {
                SocialStanding.NEUTRAL
            }
        }
    }
}
