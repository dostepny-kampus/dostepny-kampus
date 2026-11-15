/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.building

import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.building.Floor
import io.github.dostepnykampus.core.model.building.FloorList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Piętra budynku z serwera z pamięcią ostatniej udanej odpowiedzi. Wymagania: US7.2, BR-G1 */
class BuildingFloorsTest {
    @Test
    fun takesFloorsFromServer() =
        runBlocking {
            val floors = ServerFloorsRepository { ServerReply.Ok(LIST) }

            assertEquals(LIST, floors.floors("C-13"), "piętra budynku C-13")
        }

    @Test
    fun keepsLastFloorsOffline() =
        runBlocking {
            val answers =
                ArrayDeque(listOf(ServerReply.Ok(LIST), ServerReply.Offline, ServerReply.Offline, ServerReply.Missing))
            val asked = mutableListOf<String>()
            val floors =
                ServerFloorsRepository { symbol ->
                    asked += symbol
                    answers.removeFirst()
                }

            assertEquals(LIST, floors.floors("C-13"), "pierwsze pytanie o piętra C-13")
            assertEquals(LIST, floors.floors("C-13"), "powtórne pytanie o piętra C-13")
            assertNull(floors.floors("A-1"), "brak pięter A-1")
            assertNull(floors.floors("X-9"), "brak pięter X-9")
            assertEquals(listOf("C-13", "C-13", "A-1", "X-9"), asked, "zapytania o piętra w kolejności")
        }

    private companion object {
        val PARTER = Floor(id = "c13-0", label = "Parter", order = 0)
        val LIST = FloorList(source = "Politechnika Wrocławska", floors = listOf(PARTER))
    }
}
