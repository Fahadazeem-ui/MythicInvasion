package io.github.mindzard.mythicinvasion.application.society

import io.github.mindzard.mythicinvasion.domain.society.FactionState
import io.github.mindzard.mythicinvasion.domain.society.FactionType
import io.github.mindzard.mythicinvasion.domain.society.SettlementState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SocietyStateStoreTest {

    @Test
    fun factionCanBeAddedAndRetrieved() {

        val store =
            SocietyStateStore()

        val faction =
            FactionState(
                factionId = "villagers",
                type = FactionType.VILLAGER,
                displayName = "Villagers",
                population = 20
            )

        store.upsertFaction(
            faction
        )

        val current =
            store.current()

        assertEquals(
            faction,
            current.factions["villagers"]
        )
    }

    @Test
    fun settlementCanBeAddedAndRetrieved() {

        val store =
            SocietyStateStore()

        val settlement =
            SettlementState(
                settlementId = "oak_village",
                name = "Oak Village",
                worldName = "world",
                centerX = 100,
                centerY = 64,
                centerZ = 200,
                population = 12
            )

        store.upsertSettlement(
            settlement
        )

        assertEquals(
            settlement,
            store.current()
                .settlements["oak_village"]
        )
    }

    @Test
    fun factionRemovalWorks() {

        val store =
            SocietyStateStore()

        store.upsertFaction(
            FactionState(
                factionId = "pillagers",
                type = FactionType.PILLAGER,
                displayName = "Pillagers"
            )
        )

        store.removeFaction(
            "pillagers"
        )

        assertNull(
            store.current()
                .factions["pillagers"]
        )
    }

    @Test
    fun settlementRemovalWorks() {

        val store =
            SocietyStateStore()

        store.upsertSettlement(
            SettlementState(
                settlementId = "oak_village",
                name = "Oak Village",
                worldName = "world",
                centerX = 0,
                centerY = 64,
                centerZ = 0
            )
        )

        store.removeSettlement(
            "oak_village"
        )

        assertTrue(
            "oak_village" !in
                store.current().settlements
        )
    }
}
