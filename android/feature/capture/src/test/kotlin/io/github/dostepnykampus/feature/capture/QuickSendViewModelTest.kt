/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.buildings.BuildingFloors
import io.github.dostepnykampus.core.data.network.NetworkStatus
import io.github.dostepnykampus.core.data.submissions.PendingPhotoDraft
import io.github.dostepnykampus.core.location.LocationSwitch
import io.github.dostepnykampus.core.location.PositionFix
import io.github.dostepnykampus.core.location.PositionSource
import io.github.dostepnykampus.core.model.BarrierGroup
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionFloor
import io.github.dostepnykampus.feature.capture.navigation.ReportRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import kotlin.coroutines.EmptyCoroutineContext

/** Krok 3 V4: bieżąca pozycja, piętro tylko w konturze i jeden zapis w kolejce (US4.1, US4.7, BR-14, NFR-7). */
@OptIn(ExperimentalCoroutinesApi::class)
class QuickSendViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val fixes = MutableSharedFlow<PositionFix>()
    private var source: Flow<PositionFix> = fixes
    private val locationOn = MutableStateFlow(true)
    private val saved = mutableListOf<SubmissionContent>()
    private var failure: Exception? = null
    private val report =
        QuickReport(
            photo = PendingPhotoDraft(photoId = "zdjecie-1", path = "/zdjecia/zdjecie-1.jpg"),
            group = BarrierGroup.TOILETS,
            floorNumber = 1,
        )
    private val online = MutableStateFlow(true)
    private val viewModel by lazy {
        QuickSendViewModel(
            positions = PositionSource { source },
            locationSwitch = LocationSwitch { locationOn },
            saver = { content, _, _ ->
                failure?.let { throw it }
                saved.add(content)
            },
            networkStatus = NetworkStatus { online },
            buildingFloors = BuildingFloors { null },
            buildingLookup = BuildingLookup(BuildingCatalogRepository(EmptyCoroutineContext) { CAMPUS }, dispatcher),
        )
    }

    @BeforeEach
    fun main() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun reset() {
        Dispatchers.resetMain()
    }

    /** „Wyślij” czeka na pozycję i nie przerywa zgłoszenia upływem czasu (NFR-23). */
    @Test
    fun nfr23_send_waits_for_position() =
        runTest(dispatcher) {
            watch()
            viewModel.send(report)
            runCurrent()
            assertEquals(QuickLookup.Locating, viewModel.lookup.value)
            assertEquals(emptyList<SubmissionContent>(), saved)
        }

    /**
     * W konturze budynku wybrane piętro trafia do treści zgłoszenia; budynek dopisuje serwer z konturu (BR-G2).
     * Scenariusz (częściowo): zgloszenie-szybkie.feature: Wybrane piętro trafia do zgłoszenia
     */
    @Test
    fun brg2_floor_inside_contour() =
        runTest(dispatcher) {
            watch()
            fix(INSIDE_A1)
            viewModel.send(report)
            runCurrent()
            assertEquals(QuickSendStage.SENT, viewModel.stage.value)
            assertEquals(listOf(SubmissionFloor(number = 1)), saved.map { it.floor })
        }

    /** Następny odczyt nie zmienia budynku używanego w zgłoszeniu V4 (US4.1). */
    @Test
    fun us4_1_keeps_first_position() =
        runTest(dispatcher) {
            watch()
            fix(INSIDE_A1)
            fix(OUTSIDE)
            assertEquals(INSIDE_A1, (viewModel.lookup.value as QuickLookup.Found).position.point)
        }

    /**
     * Poza konturem zgłoszenie zapisuje się z bieżącą pozycją, bez piętra i bez błędu (BR-14, BR-G2).
     * Scenariusz: budynki-bez-szczegolow.feature: Zgłoszenie szybkie poza konturem budynku zapisuje się bez błędu
     * Scenariusz: zgloszenie-szybkie.feature: Pozycja poza konturem przy wysłaniu zostawia piętro puste
     */
    @Test
    fun br14_no_floor_outside() =
        runTest(dispatcher) {
            watch()
            fix(OUTSIDE)
            viewModel.send(report)
            runCurrent()
            assertEquals(listOf(null), saved.map { it.floor })
            assertEquals(listOf(OUTSIDE), saved.map { it.location })
            assertEquals(listOf(null), saved.map { it.buildingSymbol })
            assertEquals(QuickSendStage.SENT, viewModel.stage.value)
        }

    /** Zgłoszenie z „Zgłoś barierę przy wejściu” trafia do kolejki z wejściem (US4.6). */
    @Test
    fun us4_6_entrance_in_queue() =
        runTest(dispatcher) {
            val route =
                ReportRoute(
                    step = ReportStep.SEND,
                    entranceId = "node/6",
                    photoId = "zdjecie-1",
                    photoPath = "/zdjecia/zdjecie-1.jpg",
                    group = BarrierGroup.ENTRANCE_AND_DOORS,
                )
            watch()
            fix(INSIDE_A1)
            viewModel.send(requireNotNull(route.quickReport()))
            runCurrent()
            assertEquals(listOf("node/6"), saved.map { it.entranceId })
        }

    /** Błąd odbiornika → „Brak pozycji” bez zegara; „Spróbuj ponownie” ustala pozycję od nowa (UC7 4c, NFR-23). */
    @Test
    fun uc7_4c_source_error_then_retry() =
        runTest(dispatcher) {
            source = flow { throw SecurityException("brak zgody") }
            watch()
            assertEquals(QuickLookup.Lost, viewModel.lookup.value)
            viewModel.send(report)
            runCurrent()
            assertEquals(emptyList<SubmissionContent>(), saved)
            source = fixes
            viewModel.retry()
            runCurrent()
            assertEquals(QuickLookup.Locating, viewModel.lookup.value)
            fix(INSIDE_A1)
            viewModel.send(report)
            runCurrent()
            assertEquals(1, saved.size)
        }

    /** Wyłączona lokalizacja → „Brak pozycji”; po włączeniu ustalanie wraca samo (UC7 4c). */
    @Test
    fun uc7_4c_location_off_then_on() =
        runTest(dispatcher) {
            locationOn.value = false
            watch()
            assertEquals(QuickLookup.Lost, viewModel.lookup.value)
            locationOn.value = true
            runCurrent()
            assertEquals(QuickLookup.Locating, viewModel.lookup.value)
        }

    /** Wysłanie bez internetu: zapis w kolejce i potwierdzenie „Zapisaliśmy zdjęcie w telefonie…” (US4.2, NFR-5). */
    @Test
    fun us4_2_saved_offline() =
        runTest(dispatcher) {
            online.value = false
            watch()
            fix(INSIDE_A1)
            viewModel.send(report)
            runCurrent()
            assertEquals(QuickSendStage.SAVED_OFFLINE, viewModel.stage.value)
            assertEquals(1, saved.size)
        }

    /** Drugie „Wyślij” nie wysyła zdjęcia drugi raz (US4.7). */
    @Test
    fun us4_7_sent_once() =
        runTest(dispatcher) {
            watch()
            fix(INSIDE_A1)
            viewModel.send(report)
            viewModel.send(report)
            runCurrent()
            viewModel.send(report)
            runCurrent()
            assertEquals(1, saved.size)
        }

    /** Pełna pamięć: „Spróbuj ponownie” zostaje (NFR-7). */
    @Test
    fun nfr7_failed_save() =
        runTest(dispatcher) {
            failure = IOException("pełna pamięć")
            watch()
            fix(INSIDE_A1)
            viewModel.send(report)
            runCurrent()
            assertEquals(QuickSendStage.FAILED, viewModel.stage.value)
        }

    private fun TestScope.watch() = backgroundScope.launch { viewModel.lookup.collect {} }.also { runCurrent() }

    private fun TestScope.fix(point: GeoPoint) {
        backgroundScope.launch { fixes.emit(PositionFix(point, 6.0, 0L)) }
        runCurrent()
    }

    private companion object {
        val INSIDE_A1 = GeoPoint(longitude = 17.0605, latitude = 51.1105)
        val OUTSIDE = GeoPoint(longitude = 17.10, latitude = 51.20)
        const val CAMPUS =
            """{"type":"FeatureCollection","attribution":"© OpenStreetMap","features":[""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["A-1"],"name":"A-1",""" +
                """"osm_id":"way/A-1"},"geometry":{"type":"Polygon","coordinates":[[[17.06,51.11],""" +
                """[17.061,51.11],[17.061,51.111],[17.06,51.111],[17.06,51.11]]]}}]}"""
    }
}
