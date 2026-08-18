package io.github.mindzard.mythicinvasion.application.behaviour

import io.github.mindzard.mythicinvasion.domain.behaviour.BehaviourAction
import io.github.mindzard.mythicinvasion.domain.behaviour.BehaviourEvent
import io.github.mindzard.mythicinvasion.domain.behaviour.PlayerBehaviourProfile
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class BehaviourProfileStore(
    private val decayEngine: BehaviourDecayEngine
) {

    private val profiles =
        ConcurrentHashMap<UUID, PlayerBehaviourProfile>()

    fun apply(
        event: BehaviourEvent
    ) {

        profiles.compute(
            event.playerId
        ) { _, existing ->

            val current =
                existing ?: PlayerBehaviourProfile(
                    playerId = event.playerId,
                    lastDecayMillis = event.timestampMillis
                )

            val currentTimestamp =
                event.timestampMillis

            val lastDecayTimestamp =
                current.lastDecayMillis

            val elapsedMillis =
                (
                    currentTimestamp -
                        lastDecayTimestamp
                    )
                    .coerceAtLeast(0L)

            /*
             * First decay all existing weighted signals according
             * to the time that has passed since their last update.
             */
            val decayedMovements =
                decayEngine.decay(
                    value = current.weightedMovements,
                    elapsedMillis = elapsedMillis
                )

            val decayedBlocksBroken =
                decayEngine.decay(
                    value = current.weightedBlocksBroken,
                    elapsedMillis = elapsedMillis
                )

            val decayedBlocksPlaced =
                decayEngine.decay(
                    value = current.weightedBlocksPlaced,
                    elapsedMillis = elapsedMillis
                )

            val decayedCombatActions =
                decayEngine.decay(
                    value = current.weightedCombatActions,
                    elapsedMillis = elapsedMillis
                )

            /*
             * The newly observed event receives full influence = 1.0.
             */
            current.copy(

                totalEvents =
                    current.totalEvents + 1L,

                joins =
                    current.joins +
                        if (
                            event.action ==
                            BehaviourAction.JOIN
                        ) {
                            1L
                        } else {
                            0L
                        },

                quits =
                    current.quits +
                        if (
                            event.action ==
                            BehaviourAction.QUIT
                        ) {
                            1L
                        } else {
                            0L
                        },

                movements =
                    current.movements +
                        if (
                            event.action ==
                            BehaviourAction.MOVE
                        ) {
                            1L
                        } else {
                            0L
                        },

                blocksBroken =
                    current.blocksBroken +
                        if (
                            event.action ==
                            BehaviourAction.BLOCK_BREAK
                        ) {
                            1L
                        } else {
                            0L
                        },

                blocksPlaced =
                    current.blocksPlaced +
                        if (
                            event.action ==
                            BehaviourAction.BLOCK_PLACE
                        ) {
                            1L
                        } else {
                            0L
                        },

                combatActions =
                    current.combatActions +
                        if (
                            event.action ==
                            BehaviourAction.COMBAT
                        ) {
                            1L
                        } else {
                            0L
                        },

                weightedMovements =
                    decayedMovements +
                        if (
                            event.action ==
                            BehaviourAction.MOVE
                        ) {
                            1.0
                        } else {
                            0.0
                        },

                weightedBlocksBroken =
                    decayedBlocksBroken +
                        if (
                            event.action ==
                            BehaviourAction.BLOCK_BREAK
                        ) {
                            1.0
                        } else {
                            0.0
                        },

                weightedBlocksPlaced =
                    decayedBlocksPlaced +
                        if (
                            event.action ==
                            BehaviourAction.BLOCK_PLACE
                        ) {
                            1.0
                        } else {
                            0.0
                        },

                weightedCombatActions =
                    decayedCombatActions +
                        if (
                            event.action ==
                            BehaviourAction.COMBAT
                        ) {
                            1.0
                        } else {
                            0.0
                        },

                lastSeenMillis =
                    currentTimestamp,

                lastDecayMillis =
                    currentTimestamp
            )
        }
    }

    /**
     * Applies time decay to an inactive player's profile.
     *
     * This is important because a player may remain offline for a
     * long period, meaning no new event arrives to trigger normal
     * per-event decay.
     */
    fun applyTimeDecay(
        nowMillis: Long
    ) {

        profiles.replaceAll { _, current ->

            val referenceTimestamp =
                if (current.lastDecayMillis > 0L) {
                    current.lastDecayMillis
                } else {
                    current.lastSeenMillis
                }

            val elapsedMillis =
                (
                    nowMillis -
                        referenceTimestamp
                    )
                    .coerceAtLeast(0L)

            if (elapsedMillis == 0L) {
                return@replaceAll current
            }

            current.copy(

                weightedMovements =
                    decayEngine.decay(
                        current.weightedMovements,
                        elapsedMillis
                    ),

                weightedBlocksBroken =
                    decayEngine.decay(
                        current.weightedBlocksBroken,
                        elapsedMillis
                    ),

                weightedBlocksPlaced =
                    decayEngine.decay(
                        current.weightedBlocksPlaced,
                        elapsedMillis
                    ),

                weightedCombatActions =
                    decayEngine.decay(
                        current.weightedCombatActions,
                        elapsedMillis
                    ),

                lastDecayMillis =
                    nowMillis
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
                features =
                    featureEngine.calculate(
                        current
                    )
            )
        }
    }

    fun recalculateAllFeatures(
        featureEngine: BehaviourFeatureEngine
    ) {

        profiles.replaceAll { _, current ->

            current.copy(
                features =
                    featureEngine.calculate(
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
