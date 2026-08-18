package io.github.mindzard.mythicinvasion.infrastructure.paper.command

import io.github.mindzard.mythicinvasion.application.society.PillagerStrategyStore
import io.github.mindzard.mythicinvasion.application.society.SettlementSocialStore
import io.github.mindzard.mythicinvasion.application.society.SocietyStateStore
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class SocietyDebugCommand(
    private val societyStateStore: SocietyStateStore,
    private val socialStore: SettlementSocialStore,
    private val pillagerStrategyStore: PillagerStrategyStore
) : CommandExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {

        if (
            args.isNotEmpty() &&
            args[0].equals(
                "player",
                ignoreCase = true
            )
        ) {
            return handlePlayerLookup(
                sender,
                args
            )
        }

        if (
            args.isNotEmpty() &&
            args[0].equals(
                "pillagers",
                ignoreCase = true
            )
        ) {
            return handlePillagerLookup(
                sender
            )
        }

        return handleOverview(
            sender
        )
    }

    private fun handleOverview(
        sender: CommandSender
    ): Boolean {

        val state =
            societyStateStore.current()

        val socialProfiles =
            socialStore
                .snapshot()
                .associateBy {
                    it.settlementId
                }

        sender.sendMessage(
            "${ChatColor.GOLD}=== MythicInvasion Society ==="
        )

        sender.sendMessage(
            "${ChatColor.YELLOW}" +
                "Settlements: " +
                state.settlements.size
        )

        if (
            state.settlements.isEmpty()
        ) {

            sender.sendMessage(
                "${ChatColor.GRAY}" +
                    "No settlements detected yet."
            )

            return true
        }

        state.settlements.values
            .sortedBy {
                it.name
            }
            .forEach { settlement ->

                val social =
                    socialProfiles[
                        settlement.settlementId
                    ]

                sender.sendMessage(
                    "${ChatColor.AQUA}" +
                        settlement.name
                )

                sender.sendMessage(
                    "${ChatColor.GRAY}" +
                        "  World: " +
                        settlement.worldName
                )

                sender.sendMessage(
                    "${ChatColor.GRAY}" +
                        "  Population: " +
                        settlement.population
                )

                sender.sendMessage(
                    "${ChatColor.GRAY}" +
                        "  Iron Golems: " +
                        settlement.guardCount
                )

                sender.sendMessage(
                    "${ChatColor.GRAY}" +
                        "  Safety: " +
                        formatPercent(
                            settlement.safetyLevel
                        )
                )

                sender.sendMessage(
                    "${ChatColor.GRAY}" +
                        "  Prosperity: " +
                        formatPercent(
                            settlement.prosperityLevel
                        )
                )

                if (
                    social != null
                ) {

                    sender.sendMessage(
                        "${ChatColor.GRAY}" +
                            "  Known Players: " +
                            social.knownPlayerCount
                    )

                    sender.sendMessage(
                        "${ChatColor.GREEN}" +
                            "  Trusted: " +
                            social.trustedPlayers.size
                    )

                    sender.sendMessage(
                        "${ChatColor.YELLOW}" +
                            "  Neutral: " +
                            social.neutralPlayers.size
                    )

                    sender.sendMessage(
                        "${ChatColor.RED}" +
                            "  Hostile/Distrusted: " +
                            social.hostilePlayers.size
                    )

                    sender.sendMessage(
                        "${ChatColor.GRAY}" +
                            "  Average Trust: " +
                            formatPercent(
                                social.averageTrust
                            )
                    )

                    sender.sendMessage(
                        "${ChatColor.GRAY}" +
                            "  Average Threat: " +
                            formatPercent(
                                social.averageThreat
                            )
                    )
                }

                sender.sendMessage("")
            }

        return true
    }

    private fun handlePillagerLookup(
        sender: CommandSender
    ): Boolean {

        val state =
            societyStateStore.current()

        val strategy =
            pillagerStrategyStore.current()

        val faction =
            state.factions[
                "pillagers"
            ]

        sender.sendMessage(
            "${ChatColor.DARK_RED}" +
                "=== Pillager Faction Intelligence ==="
        )

        sender.sendMessage(
            "${ChatColor.GRAY}" +
                "Population: " +
                (faction?.population ?: 0)
        )

        sender.sendMessage(
            "${ChatColor.GRAY}" +
                "Military Strength: " +
                formatPercent(
                    faction?.militaryStrength
                        ?: 0.0
                )
        )

        sender.sendMessage(
            "${ChatColor.GRAY}" +
                "Influence: " +
                formatPercent(
                    faction?.influence
                        ?: 0.0
                )
        )

        val targetName =
            strategy.selectedSettlementName

        if (
            targetName == null
        ) {

            sender.sendMessage(
                "${ChatColor.GRAY}" +
                    "Target: None"
            )

            sender.sendMessage(
                "${ChatColor.GRAY}" +
                    "Reason: " +
                    strategy.targetReason
            )

            return true
        }

        sender.sendMessage(
            "${ChatColor.RED}" +
                "Target: " +
                targetName
        )

        sender.sendMessage(
            "${ChatColor.GRAY}" +
                "Target Score: " +
                formatPercent(
                    strategy.targetScore
                )
        )

        sender.sendMessage(
            "${ChatColor.GRAY}" +
                "Reason: " +
                strategy.targetReason
        )

        sender.sendMessage(
            "${ChatColor.GRAY}" +
                "Pressure Units: " +
                strategy.assignedPillagerCount
        )

        sender.sendMessage(
            "${ChatColor.GRAY}" +
                "Scout Units: " +
                strategy.scoutingPillagerCount
        )

        return true
    }

    private fun handlePlayerLookup(
        sender: CommandSender,
        args: Array<out String>
    ): Boolean {

        if (
            args.size < 2
        ) {

            sender.sendMessage(
                "${ChatColor.RED}" +
                    "Usage: /society player <name>"
            )

            return true
        }

        val targetName =
            args[1]

        val targetPlayer =
            Bukkit.getOfflinePlayer(
                targetName
            )

        val targetId =
            targetPlayer.uniqueId

        val state =
            societyStateStore.current()

        sender.sendMessage(
            "${ChatColor.GOLD}" +
                "=== Society Profile: " +
                targetName +
                " ==="
        )

        var found =
            false

        state.settlements.values
            .sortedBy {
                it.name
            }
            .forEach { settlement ->

                val relations =
                    socialStore
                        .snapshot()
                        .firstOrNull {
                            it.settlementId ==
                                settlement.settlementId
                        }

                if (
                    relations == null
                ) {
                    return@forEach
                }

                val trust =
                    relations.trustedPlayers[
                        targetId
                    ]

                val neutral =
                    relations.neutralPlayers[
                        targetId
                    ]

                val hostile =
                    relations.hostilePlayers[
                        targetId
                    ]

                if (
                    trust == null &&
                    neutral == null &&
                    hostile == null
                ) {
                    return@forEach
                }

                found =
                    true

                val standing =
                    when {
                        trust != null ->
                            "TRUSTED"

                        hostile != null ->
                            "HOSTILE / DISTRUSTED"

                        else ->
                            "NEUTRAL"
                    }

                sender.sendMessage(
                    "${ChatColor.AQUA}" +
                        settlement.name
                )

                sender.sendMessage(
                    "${ChatColor.GRAY}" +
                        "  Standing: " +
                        standing
                )

                if (
                    trust != null
                ) {

                    sender.sendMessage(
                        "${ChatColor.GRAY}" +
                            "  Trust: " +
                            formatPercent(
                                trust
                            )
                    )
                }

                if (
                    hostile != null
                ) {

                    sender.sendMessage(
                        "${ChatColor.GRAY}" +
                            "  Threat: " +
                            formatPercent(
                                hostile
                            )
                    )
                }
            }

        if (
            !found
        ) {

            sender.sendMessage(
                "${ChatColor.GRAY}" +
                    "No recorded relationship with this player."
            )
        }

        return true
    }

    private fun formatPercent(
        value: Double
    ): String {

        return "${
            "%.0f".format(
                value * 100.0
            )
        }%"
    }
}
