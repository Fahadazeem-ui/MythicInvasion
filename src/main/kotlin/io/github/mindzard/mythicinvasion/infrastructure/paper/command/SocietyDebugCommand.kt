package io.github.mindzard.mythicinvasion.infrastructure.paper.command

import io.github.mindzard.mythicinvasion.application.society.SocietyStateStore
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class SocietyDebugCommand(
    private val societyStateStore: SocietyStateStore
) : CommandExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {

        val state =
            societyStateStore.current()

        sender.sendMessage(
            "${ChatColor.GOLD}=== MythicInvasion Society ==="
        )

        sender.sendMessage(
            "${ChatColor.YELLOW}" +
                "Settlements: ${state.settlements.size}"
        )

        if (
            state.settlements.isEmpty()
        ) {

            sender.sendMessage(
                "${ChatColor.GRAY}" +
                    "No settlements detected yet."
            )

            sender.sendMessage(
                "${ChatColor.GRAY}" +
                    "The scanner will update automatically."
            )

            return true
        }

        state.settlements.values
            .sortedBy { it.name }
            .forEach { settlement ->

                sender.sendMessage(
                    "${ChatColor.AQUA}" +
                        settlement.name
                )

                sender.sendMessage(
                    "${ChatColor.GRAY}" +
                        "  World: ${settlement.worldName}"
                )

                sender.sendMessage(
                    "${ChatColor.GRAY}" +
                        "  Population: ${settlement.population}"
                )

                sender.sendMessage(
                    "${ChatColor.GRAY}" +
                        "  Iron Golems: ${settlement.guardCount}"
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

                sender.sendMessage("")
            }

        return true
    }

    private fun formatPercent(
        value: Double
    ): String {

        return "${"%.0f".format(value * 100.0)}%"
    }
}
