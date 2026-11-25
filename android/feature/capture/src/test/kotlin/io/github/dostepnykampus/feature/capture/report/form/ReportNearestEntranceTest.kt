/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report.form

import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.feature.capture.report.A1
import io.github.dostepnykampus.feature.capture.report.ReportFormViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.coroutines.EmptyCoroutineContext

/** Najbliższe wejście budynku do opisu miejsca słowami na C2. Wymagania: US7.4, US6.1, NFR-19 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReportNearestEntranceTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun main() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun reset() {
        Dispatchers.resetMain()
    }

    /** Wiersz: C2 w budynku z wejściami liczy najbliższe wejście tego budynku, nie sąsiada. Wymagania: US7.4 */
    @Test
    fun findsNearestOwnEntrance() =
        runTest(dispatcher) {
            val viewModel = opened(A1)
            runCurrent()

            val nearest = checkNotNull(state(viewModel).form.place.nearestEntrance)
            assertEquals("n/1", nearest.entrance.id, "najbliższe wejście budynku A-1")
            assertTrue(nearest.meters < NEAR_METERS, "odległość od punktu pomiaru")
            viewModel.back()
            assertEquals(ReportStage.LEFT, state(viewModel).stage, "wejście nie jest zmianą Zbierającego")
        }

    /** Wiersz: poza budynkiem C2 nie podaje wejścia. Wymagania: US6.1, BR-G4 */
    @Test
    fun skipsOutside() =
        runTest(dispatcher) {
            val viewModel = opened(A1.copy(buildingSymbol = null))
            runCurrent()

            assertNull(state(viewModel).form.place.nearestEntrance, "bez budynku bez wejścia")
        }

    private fun opened(place: ReportPlace): ReportFormViewModel =
        ReportFormViewModel(catalog = BuildingCatalogRepository(EmptyCoroutineContext) { CATALOG }) { _, _, _ -> }
            .apply { open(ReportForm(place = place)) }

    private fun state(viewModel: ReportFormViewModel): ReportUiState = checkNotNull(viewModel.uiState.value)

    private companion object {
        const val NEAR_METERS = 20.0
        const val CATALOG =
            """{"type":"FeatureCollection","attribution":"© plik","features":[""" +
                """{"type":"Feature","properties":{"kind":"entrance","symbols":["A-1"],"osm_id":"n/1"},""" +
                """"geometry":{"type":"Point","coordinates":[17.0621,51.1079]}},""" +
                """{"type":"Feature","properties":{"kind":"entrance","symbols":["A-1"],"osm_id":"n/2"},""" +
                """"geometry":{"type":"Point","coordinates":[17.063,51.1085]}},""" +
                """{"type":"Feature","properties":{"kind":"entrance","symbols":["B-8"],"osm_id":"n/3"},""" +
                """"geometry":{"type":"Point","coordinates":[17.06209,51.10785]}}]}"""
    }
}
