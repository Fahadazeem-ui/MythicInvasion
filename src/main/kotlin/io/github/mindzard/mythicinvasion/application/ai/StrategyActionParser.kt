package io.github.mindzard.mythicinvasion.application.ai

import io.github.mindzard.mythicinvasion.domain.ai.StrategyAction

class StrategyActionParser {

    fun parse(
        rawAction: String
    ): StrategyAction {

        val normalized =
            rawAction
                .trim()
                .uppercase()
                .replace(
                    " ",
                    "_"
                )
                .replace(
                    "-",
                    "_"
                )

        return when (normalized) {

            "INCREASE_HOSTILE_PRESSURE" ->
                StrategyAction.INCREASE_HOSTILE_PRESSURE

            "FOCUS_HIGH_PRESSURE_PLAYERS" ->
                StrategyAction.FOCUS_HIGH_PRESSURE_PLAYERS

            "ADAPTIVE_HOSTILE_TARGETING" ->
                StrategyAction.ADAPTIVE_HOSTILE_TARGETING

            "DEFEND_SETTLEMENTS" ->
                StrategyAction.DEFEND_SETTLEMENTS

            "SCOUT_SETTLEMENTS" ->
                StrategyAction.SCOUT_SETTLEMENTS

            else ->
                StrategyAction.NONE
        }
    }
}
