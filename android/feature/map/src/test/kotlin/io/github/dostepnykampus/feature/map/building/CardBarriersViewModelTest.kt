/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.data.barrier.AcceptedBarriers
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.Severity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.coroutines.EmptyCoroutineContext

/** Zgłoszenia przyjęte na stanie karty V2: tylko te z budynku karty. Wymagania: US2.2, US2.4 */
@OptIn(ExperimentalCoroutinesApi::class)
class CardBarriersViewModelTest {
    @BeforeEach
    fun main() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterEach
    fun reset() {
        Dispatchers.resetMain()
    }

    /** Wiersz: bariery z serwera → stan karty ma tylko zgłoszenia budynku karty, bez barier innych i poza budynkiem. */
    @Test
    fun us2_2_only_own_building() =
        runTest {
            val all =
                listOf(
                    barrier(id = "z1", symbol = "A-1"),
                    barrier(id = "z2", symbol = "B-4"),
                    barrier(id = "z3", symbol = null),
                )
            assertEquals(listOf("z1"), cardBarriers(all))
        }

    /** Wiersz: brak zgłoszeń przyjętych → karta ma pustą listę barier. Wymagania: US2.4 */
    @Test
    fun us2_4_none_is_empty() =
        runTest {
            assertEquals(emptyList<String>(), cardBarriers(emptyList()))
        }

    private fun TestScope.cardBarriers(all: List<AcceptedBarrier>): List<String> {
        val catalog = BuildingCatalogRepository(EmptyCoroutineContext) { ASSET_JSON }
        val card =
            BuildingCardViewModel(
                symbol = "A-1",
                repository = catalog,
                networkStatus = { flowOf(true) },
                acceptedBarriers = AcceptedBarriers { all },
            )
        backgroundScope.launch { card.uiState.collect {} }
        advanceUntilIdle()
        val ready = card.uiState.value as BuildingCardUiState.Ready
        return ready.barriers.map { it.id }
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
