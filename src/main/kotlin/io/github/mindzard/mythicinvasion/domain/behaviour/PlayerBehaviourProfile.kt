package io.github.mindzard.mythicinvasion.application.behaviour

import io.github.mindzard.mythicinvasion.domain.behaviour.BehaviourAction
import io.github.mindzard.mythicinvasion.domain.behaviour.BehaviourEvent
import io.github.mindzard.mythicinvasion.domain.behaviour.PlayerBehaviourProfile
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class BehaviourProfileStore {

    private val profiles =
        ConcurrentHashMap<UUID, PlayerBehaviourProfile>()

    fun apply(event: BehaviourEvent) {

        profiles.compute(
            event.playerId
        ) { _, existing ->

            val current =
                existing ?: PlayerBehaviourProfile(
                    playerId = event.playerId
                )

            current.copy(
                totalEvents = current.totalEvents + 1L,

                joins = current.joins +
                    if (event.action == BehaviourAction.JOIN) {
                        1L
                    } else {
                        0L
                    },

                quits = current.quits +
                    if (event.action == BehaviourAction.QUIT) {
                        1L
                    } else {
                        0L
                    },

                movements = current.movements +
                    if (event.action == BehaviourAction.MOVE) {
                        1L
                    } else {
                        0L
                    },

                blocksBroken = current.blocksBroken +
                    if (event.action == BehaviourAction.BLOCK_BREAK) {
                        1L
                    } else {
                        0L
                    },

                blocksPlaced = current.blocksPlaced +
                    if (event.action == BehaviourAction.BLOCK_PLACE) {
                        1L
                    } else {
                        0L
                    },

                combatActions = current.combatActions +
                    if (event.action == BehaviourAction.COMBAT) {
                        1L
                    } else {
                        0L
                    },

                lastSeenMillis = event.timestampMillis
            )
        }
    }

    fun recalculateFeatures(
        playerId: UUID,
        featureEngine: BehaviourFeatureEngine
    ) {
        profiles.computeIfPresent(
            playerId
        ) { _, current ->

            current.copy(
                features = featureEngine.calculate(
                    current
                )
            )
        }
    }

    fun get(
        playerId: UUID
    ): PlayerBehaviourProfile? {
        return profiles[playerId]
    }

    fun snapshot(): List<PlayerBehaviourProfile> {
        return profiles.values.toList()
    }

    fun remove(
        playerId: UUID
    ) {
        profiles.remove(playerId)
    }

    fun clear() {
        profiles.clear()
    }
}
