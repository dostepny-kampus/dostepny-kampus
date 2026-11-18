/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.building

import io.github.dostepnykampus.core.data.server.ServerCard
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.building.Building
import io.github.dostepnykampus.core.model.building.EvacuationExit
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Wyjścia ewakuacyjne karty budynku z serwera z pamięcią ostatniej udanej odpowiedzi. Wymagania: US2.6, NFR-10 */
class ServerEvacuationExitsTest {
    @Test
    fun takesExitsFromCard() =
        runBlocking {
            val exits = ServerEntrancesRepository { ServerReply.Ok(CARD) }

            assertEquals(listOf(EXIT), exits.evacuationExits(SYMBOL), "wyjścia ewakuacyjne budynku z serwera")
        }

    @Test
    fun keepsLastOffline() =
        runBlocking {
            val answers = ArrayDeque(listOf(ServerReply.Ok(CARD), ServerReply.Offline))
            val exits = ServerEntrancesRepository { answers.removeFirst() }

            assertEquals(
                List(2) { listOf(EXIT) },
                List(2) { exits.evacuationExits(SYMBOL) },
                "bez sieci zostają wyjścia pobrane wcześniej",
            )
        }

    @Test
    fun givesEmptyForMissingFirst() =
        runBlocking {
            assertEquals(
                emptyList<EvacuationExit>(),
                ServerEntrancesRepository { ServerReply.Missing }.evacuationExits(SYMBOL),
                "brak zasobu daje pustą listę wyjść",
            )
        }

    private companion object {
        const val SYMBOL = "A-1"
        val ORIGIN = GeoPoint(longitude = 0.0, latitude = 0.0)
        val EXIT = EvacuationExit(id = "pwr/9", location = ORIGIN, description = "Wyjście", doorWidthCm = 90)
        val CARD =
            ServerCard(
                building =
                    Building(
                        symbol = SYMBOL,
                        name = "Budynek",
                        outline = GeoPolygon(listOf(listOf(ORIGIN, GeoPoint(longitude = 1.0, latitude = 1.0), ORIGIN))),
                    ),
                entrances = emptyList(),
                evacuationExits = listOf(EXIT),
            )
    }
}
