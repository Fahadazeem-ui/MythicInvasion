package io.github.mindzard.mythicinvasion.application.society

import io.github.mindzard.mythicinvasion.domain.society.PlayerVillagerRelationship
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class VillagerRelationshipStore {

    private val relationships =
        ConcurrentHashMap<
            String,
            PlayerVillagerRelationship
            >()

    fun replaceAll(
        newRelationships:
            Collection<PlayerVillagerRelationship>
    ) {

        val replacement =
            newRelationships.associateBy {
                createKey(
                    villagerId =
                        it.villagerId,

                    playerId =
                        it.playerId
                )
            }

        relationships.clear()

        relationships.putAll(
            replacement
        )
    }

    fun get(
        villagerId: UUID,
        playerId: UUID
    ): PlayerVillagerRelationship? {

        return relationships[
            createKey(
                villagerId,
                playerId
            )
        ]
    }

    fun forPlayer(
        playerId: UUID
    ): List<PlayerVillagerRelationship> {

        return relationships
            .values
            .filter {
                it.playerId == playerId
            }
    }

    fun forSettlement(
        settlementId: String
    ): List<PlayerVillagerRelationship> {

        return relationships
            .values
            .filter {
                it.settlementId ==
                    settlementId
            }
    }

    fun snapshot():
        List<PlayerVillagerRelationship> {

        return relationships
            .values
            .toList()
    }

    fun clear() {
        relationships.clear()
    }

    private fun createKey(
        villagerId: UUID,
        playerId: UUID
    ): String {

        return "$villagerId:$playerId"
    }
}
