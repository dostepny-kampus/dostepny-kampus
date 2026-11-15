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
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Wejścia karty budynku z serwera z pamięcią ostatniej udanej odpowiedzi. Wymagania: NFR-10 */
class ServerEntrancesTest {
    @Test
    fun takesEntrancesFromCard() =
        runBlocking {
            val entrances = ServerEntrancesRepository { ServerReply.Ok(CARD) }

            assertEquals(listOf(ENTRANCE), entrances.entrances(SYMBOL), "wejścia budynku z serwera")
        }

    @Test
    fun keepsLastOffline() =
        runBlocking {
            val answers = ArrayDeque(listOf(ServerReply.Ok(CARD), ServerReply.Offline, ServerReply.Fault(SERVER_ERROR)))
            val entrances = ServerEntrancesRepository { answers.removeFirst() }

            assertEquals(
                List(3) { listOf(ENTRANCE) },
                List(3) { entrances.entrances(SYMBOL) },
                "powtórne pytania dają te same wejścia",
            )
        }

    @Test
    fun givesEmptyForMissingFirst() =
        runBlocking {
            assertEquals(
                emptyList<Entrance>(),
                ServerEntrancesRepository { ServerReply.Missing }.entrances(SYMBOL),
                "brak zasobu daje pustą listę wejść",
            )
        }

    private companion object {
        const val SYMBOL = "A-1"
        const val SERVER_ERROR = 500
        val ORIGIN = GeoPoint(longitude = 0.0, latitude = 0.0)
        val ENTRANCE =
            Entrance(
                id = "node/1",
                buildingSymbol = SYMBOL,
                name = null,
                isMain = true,
                type = null,
                location = ORIGIN,
                marking = EntranceMarking.NO_DATA,
            )
        val CARD =
            ServerCard(
                building =
                    Building(
                        symbol = SYMBOL,
                        name = "Budynek",
                        outline = GeoPolygon(listOf(listOf(ORIGIN, GeoPoint(longitude = 1.0, latitude = 1.0), ORIGIN))),
                    ),
                entrances = listOf(ENTRANCE),
            )
    }
}
