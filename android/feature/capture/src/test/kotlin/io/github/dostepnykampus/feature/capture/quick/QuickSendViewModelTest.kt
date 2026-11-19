/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.quick

import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.building.BuildingFloors
import io.github.dostepnykampus.core.data.network.NetworkStatus
import io.github.dostepnykampus.core.data.submission.sync.PendingPhotoDraft
import io.github.dostepnykampus.core.geo.CampusGrounds
import io.github.dostepnykampus.core.location.LocationSwitch
import io.github.dostepnykampus.core.location.PositionFix
import io.github.dostepnykampus.core.location.PositionSource
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.BarrierGroup
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionFloor
import io.github.dostepnykampus.feature.capture.location.BuildingLookup
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

/**
 * Krok 3 V4: bieżąca pozycja, piętro tylko w konturze i jeden zapis w kolejce.
 *
 * Wymagania: US4.1, US4.7, BR-14, NFR-7
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuickSendViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val fixes = MutableSharedFlow<PositionFix>()
    private var source: Flow<PositionFix> = fixes
    private val locationOn = MutableStateFlow(true)
    private val saved = mutableListOf<SubmissionContent>()
    private var failure: Exception? = null
    private var isOnGrounds = true
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
            buildingLookup =
                BuildingLookup(
                    catalog = BuildingCatalogRepository(EmptyCoroutineContext) { QUICK_CAMPUS },
                    computeContext = dispatcher,
                    grounds = CampusGrounds { isOnGrounds },
                ),
        ).also { it.start(entranceId = null) }
    }

    @BeforeEach
    fun main() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun reset() {
        Dispatchers.resetMain()
    }

    /** „Wyślij” czeka na pozycję i nie przerywa zgłoszenia upływem czasu. Wymagania: NFR-23 */
    @Test
    fun waitsForPosition() =
        runTest(dispatcher) {
            watch()
            viewModel.send(report)
            runCurrent()
            assertEquals(QuickLookup.Locating, viewModel.lookup.value)
            assertEquals(emptyList<SubmissionContent>(), saved)
        }

    /**
     * W konturze budynku wybrane piętro trafia do treści zgłoszenia; budynek dopisuje serwer z konturu.
     * Scenariusz (częściowo): zgloszenie-szybkie.feature: Wybrane piętro trafia do zgłoszenia
     *
     * Wymagania: BR-G2
     */
    @Test
    fun keepsFloorInsideContour() =
        runTest(dispatcher) {
            watch()
            fix(INSIDE_A1)
            viewModel.send(report)
            runCurrent()
            assertEquals(QuickSendStage.SENT, viewModel.stage.value)
            assertEquals(listOf(SubmissionFloor(number = 1)), saved.map { it.floor })
        }

    /**
     * Zgłoszenie bez wybranego wejścia bierze miejsce z bieżącej pozycji, a następny odczyt nie zmienia budynku
     * używanego w zgłoszeniu V4.
     * Scenariusz (częściowo): zgloszenie-szybkie.feature: Odnośnik w wierszu kilku wejść bez danych bierze miejsce
     *   z bieżącej pozycji
     *
     * Wymagania: US4.1, US4.6
     */
    @Test
    fun keepsFirstPosition() =
        runTest(dispatcher) {
            watch()
            fix(INSIDE_A1)
            fix(OUTSIDE)
            assertEquals(INSIDE_A1, (viewModel.lookup.value as QuickLookup.Found).position.point)
        }

    /**
     * Poza konturem zgłoszenie zapisuje się z bieżącą pozycją, bez piętra i bez błędu.
     * Scenariusz: budynki-bez-szczegolow.feature: Zgłoszenie szybkie poza konturem budynku zapisuje się bez błędu
     * Scenariusz: zgloszenie-szybkie.feature: Pozycja poza konturem przy wysłaniu zostawia piętro puste
     *
     * Wymagania: BR-14, BR-G2
     */
    @Test
    fun savesOutsideWithoutFloor() =
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

    /** Zgłoszenie z „Zgłoś barierę przy wejściu” trafia do kolejki z wejściem. Wymagania: US4.6 */
    @Test
    fun queuesEntrance() =
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

    /**
     * Błąd odbiornika → „Brak pozycji” bez zegara; „Spróbuj ponownie” ustala pozycję od nowa.
     *
     * Wymagania: UC7, NFR-23
     */
    @Test
    fun retriesAfterSourceError() =
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

    /**
     * Pozycja poza terenem PWr: „Wyślij” niczego nie zapisuje, a „Spróbuj ponownie” ustala pozycję od nowa; zdjęcie
     * i grupa zostają.
     * Scenariusz (częściowo): zgloszenie-szybkie.feature: Zgłoszenie szybkie tylko na terenie PWr
     *
     * Wymagania: US4.9, BR-G5
     */
    @Test
    fun refusesPositionOutsideGrounds() =
        runTest(dispatcher) {
            isOnGrounds = false
            watch()
            fix(OUTSIDE)
            assertEquals(QuickLookup.Outside, viewModel.lookup.value, "pozycja poza terenem PWr")
            viewModel.send(report)
            runCurrent()
            assertEquals(emptyList<SubmissionContent>(), saved, "bez zapisu poza terenem")
            isOnGrounds = true
            viewModel.retry()
            runCurrent()
            fix(OUTSIDE)
            viewModel.send(report)
            runCurrent()
            assertEquals(listOf(OUTSIDE), saved.map { it.location }, "zapis po powrocie na teren")
        }

    /** Wyłączona lokalizacja → „Brak pozycji”; po włączeniu ustalanie wraca samo. Wymagania: UC7 */
    @Test
    fun resumesWhenLocationTurnsOn() =
        runTest(dispatcher) {
            locationOn.value = false
            watch()
            assertEquals(QuickLookup.Lost, viewModel.lookup.value)
            locationOn.value = true
            runCurrent()
            assertEquals(QuickLookup.Locating, viewModel.lookup.value)
        }

    /**
     * Wysłanie bez internetu: zapis w kolejce i potwierdzenie „Zapisaliśmy zdjęcie w telefonie…”.
     *
     * Wymagania: US4.2, NFR-5
     */
    @Test
    fun savesOffline() =
        runTest(dispatcher) {
            online.value = false
            watch()
            fix(INSIDE_A1)
            viewModel.send(report)
            runCurrent()
            assertEquals(QuickSendStage.SAVED_OFFLINE, viewModel.stage.value)
            assertEquals(1, saved.size)
        }

    /** Drugie „Wyślij” nie wysyła zdjęcia drugi raz. Wymagania: US4.7 */
    @Test
    fun sendsOnce() =
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

    /** Pełna pamięć: „Spróbuj ponownie” zostaje. Wymagania: NFR-7 */
    @Test
    fun keepsRetryAfterFailedSave() =
        runTest(dispatcher) {
            failure = IOException("pełna pamięć")
            watch()
            fix(INSIDE_A1)
            viewModel.send(report)
            runCurrent()
            assertEquals(QuickSendStage.FAILED, viewModel.stage.value)
        }

    /**
     * Zgłoszenie przy wejściu bierze punkt i budynek wejścia z katalogu i nie słucha odbiornika pozycji.
     * Scenariusz (częściowo): zgloszenie-szybkie.feature: Zgłoszenie przy wejściu ma miejsce w punkcie wybranego
     *   wejścia
     *
     * Wymagania: US4.6, BR-8
     */
    @Test
    fun takesPlaceFromEntrance() =
        runTest(dispatcher) {
            source = flow { error("odbiornik pozycji przy wejściu") }
            viewModel.start(entranceId = "node/6")
            watch()
            val position = (viewModel.lookup.value as QuickLookup.Found).position
            assertEquals(ENTRANCE, position.point)
            assertEquals("A-1", position.buildingSymbol)
            assertEquals("node/6", position.entrance?.id)
        }

    private fun TestScope.watch() = backgroundScope.launch { viewModel.lookup.collect {} }.also { runCurrent() }

    private fun TestScope.fix(point: GeoPoint) {
        backgroundScope.launch { fixes.emit(PositionFix(point, 6.0, 0L)) }
        runCurrent()
    }

    private companion object {
        val INSIDE_A1 = GeoPoint(longitude = 17.0605, latitude = 51.1105)
        val OUTSIDE = GeoPoint(longitude = 17.10, latitude = 51.20)
        val ENTRANCE = GeoPoint(longitude = 17.06, latitude = 51.11)
    }
}
