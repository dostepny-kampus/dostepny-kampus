/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.core.data.barriers.AcceptedBarrier
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.Severity
import io.github.dostepnykampus.feature.map.data.campusMapData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.coroutines.EmptyCoroutineContext

/** Bariery ze zgłoszeń przyjętych w danych mapy V1, na podróbkach źródeł (US1.3, NFR-10). */
@OptIn(ExperimentalCoroutinesApi::class)
class MapBarriersLoadTest {
    private val online = MutableStateFlow(true)
    private val answers = ArrayDeque<List<AcceptedBarrier>>()
    private val repository =
        BuildingCatalogRepository(EmptyCoroutineContext) {
            """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["C-13"],"osm_id":"way/1"},""" +
                """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}]}"""
        }

    @BeforeEach
    fun main() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun reset() {
        Dispatchers.resetMain()
    }

    /** Wiersz: V1, bariery ze zgłoszeń przyjętych jako znaki na mapie (US1.3, BR-6). */
    @Test
    fun us1_3_barriers_in_map_data() =
        runTest(UnconfinedTestDispatcher()) {
            answers += Severity.entries.map(::barrier)

            assertEquals(Severity.entries.size, shownBarriers())
        }

    /** Wiersz: brak sieci, bariery z telefonu; po powrocie sieci mapa pyta źródło ponownie (NFR-10). */
    @Test
    fun nfr10_offline_then_online() =
        runTest(UnconfinedTestDispatcher()) {
            online.value = false
            answers += listOf(barrier(Severity.BLOCKING))
            answers += Severity.entries.map(::barrier)
            val viewModel = started()

            assertEquals(1, viewModel.barrierCount())
            online.value = true
            assertEquals(Severity.entries.size, viewModel.barrierCount())
        }

    /** Wiersz: zgłoszenie, które przestało być przyjęte, znika z mapy po kolejnym pobraniu (US1.3). */
    @Test
    fun us1_3_no_longer_accepted_gone() =
        runTest(UnconfinedTestDispatcher()) {
            answers += listOf(barrier(Severity.BLOCKING))
            answers += listOf(barrier(Severity.BLOCKING))
            answers += emptyList<AcceptedBarrier>()
            val viewModel = started()

            assertEquals(1, viewModel.barrierCount())
            online.value = false
            online.value = true
            assertEquals(0, viewModel.barrierCount())
        }

    private fun TestScope.shownBarriers(): Int = started().barrierCount()

    private fun TestScope.started(): CampusMapViewModel {
        val viewModel =
            CampusMapViewModel(
                repository = repository,
                networkStatus = { online },
                mapListChoice = TestMapListChoice(),
                computeContext = EmptyCoroutineContext,
                geometry = ::campusMapData,
                barriers = { answers.removeFirst() },
            )
        backgroundScope.launch { viewModel.uiState.collect {} }
        return viewModel
    }

    private fun CampusMapViewModel.barrierCount(): Int {
        val barriers = checkNotNull(uiState.value.mapData).barriers
        val features = barriers.jsonObject.getValue("features")
        return features.jsonArray.size
    }

    private fun barrier(severity: Severity) =
        AcceptedBarrier(
            id = severity.name,
            location = GeoPoint(longitude = 0.5, latitude = 0.5),
            severity = severity,
            type = null,
            buildingSymbol = "C-13",
            verifiedAt = null,
        )
}
