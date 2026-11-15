/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.lifecycle.viewModelScope
import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.data.barrier.AcceptedBarriers
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.Severity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.coroutines.EmptyCoroutineContext

/** Zgłoszenia przyjęte na stanie karty V2: tylko te z budynku karty. Wymagania: US2.2, US2.4, US2.7 */
@OptIn(ExperimentalCoroutinesApi::class)
class CardBarriersViewModelTest {
    /** Wiersz: bariery z serwera → stan karty ma tylko zgłoszenia budynku karty, bez barier innych i poza budynkiem. */
    @Test
    fun keepsOnlyOwnBuilding() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val all =
                listOf(
                    barrier(id = "z1", symbol = "A-1"),
                    barrier(id = "z2", symbol = "B-4"),
                    barrier(id = "z3", symbol = null),
                )
            val ids = cardBarriers(all)?.map { it.id }
            Dispatchers.resetMain()
            assertEquals(listOf("z1"), ids, "Karta ma tylko zgłoszenia swojego budynku")
        }

    /** Wiersz: brak zgłoszeń przyjętych → karta ma pustą listę barier. Wymagania: US2.4 */
    @Test
    fun treatsNoneAsEmpty() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val barriers = cardBarriers(emptyList())
            Dispatchers.resetMain()
            assertEquals(
                emptyList<AcceptedBarrier>(),
                barriers,
                "Bez zgłoszeń przyjętych lista barier karty jest pusta",
            )
        }

    /** Wiersz: bariery nie są ani z serwera, ani z kopii → stan karty nie ma danych o barierach. Wymagania: US2.7 */
    @Test
    fun us2_7_not_synced_is_no_data() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val barriers = cardBarriers(listOf(barrier(id = "z1", symbol = "A-1")), syncedAt = null)
            Dispatchers.resetMain()
            assertNull(barriers, "Bez pobrania barier karta nie ma danych o barierach")
        }

    private fun TestScope.cardBarriers(
        all: List<AcceptedBarrier>,
        syncedAt: Instant? = Instant.parse("2026-11-12T10:00:00Z"),
    ): List<AcceptedBarrier>? {
        val catalog = BuildingCatalogRepository(EmptyCoroutineContext) { ASSET_JSON }
        val card =
            BuildingCardViewModel(
                symbol = "A-1",
                repository = catalog,
                networkStatus = { flowOf(true) },
                acceptedBarriers =
                    object : AcceptedBarriers {
                        override suspend fun barriers() = all

                        override fun syncedAt() = syncedAt
                    },
            )
        val collector = backgroundScope.launch { card.uiState.collect {} }
        advanceUntilIdle()
        val ready = card.uiState.value as BuildingCardUiState.Ready
        collector.cancel()
        card.viewModelScope.cancel()
        advanceUntilIdle()
        return ready.barriers
    }

    private fun barrier(id: String, symbol: String?) =
        AcceptedBarrier(
            id = id,
            location = GeoPoint(longitude = 0.5, latitude = 0.0),
            severity = Severity.BLOCKING,
            type = null,
            buildingSymbol = symbol,
            verifiedAt = null,
        )

    private companion object {
        const val ASSET_JSON =
            """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["A-1"],"display_name":"Gmach",""" +
                """"osm_id":"way/1"},"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}]}"""
    }
}
