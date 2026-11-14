/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.measure

import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.location.ElapsedClock
import io.github.dostepnykampus.core.location.GnssSystem
import io.github.dostepnykampus.core.location.LocationSwitch
import io.github.dostepnykampus.core.location.PositionFix
import io.github.dostepnykampus.core.location.PositionSource
import io.github.dostepnykampus.core.location.Satellite
import io.github.dostepnykampus.core.location.SatelliteSource
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.capture.location.BuildingLookup
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.coroutines.CoroutineContext

/**
 * Pomiar na ekranie C1 na odczytach testowych w czasie wirtualnym, bez odbiornika GNSS.
 *
 * Wymagania: US6.1, NFR-23, NFR-27
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MeasureViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val catalogDispatcher = StandardTestDispatcher(dispatcher.scheduler)
    private var lookupDispatches = 0
    private val lookupDispatcher =
        object : CoroutineDispatcher() {
            override fun dispatch(context: CoroutineContext, block: Runnable) {
                lookupDispatches += 1
                dispatcher.dispatch(context, block)
            }
        }
    private var catalogReads = 0
    private val fixes = MutableSharedFlow<PositionFix>(extraBufferCapacity = 1)
    private val satellites = MutableSharedFlow<List<Satellite>>(extraBufferCapacity = 1)
    private var failure: RuntimeException? = null
    private val locationOn = MutableStateFlow(true)
    private val clock = ElapsedClock { BOOT + dispatcher.scheduler.currentTime }
    private val viewModel by lazy {
        MeasureViewModel(
            positions = PositionSource { failure?.let { error -> flow { throw error } } ?: fixes },
            satellites = SatelliteSource { satellites },
            clock = clock,
            locationSwitch = LocationSwitch { locationOn },
            buildingLookup =
                BuildingLookup(
                    BuildingCatalogRepository(catalogDispatcher) {
                        catalogReads += 1
                        CAMPUS
                    },
                    lookupDispatcher,
                ),
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

    /** „Szukamy satelitów”, dopóki nie ma odczytu ze znaną dokładnością. */
    @Test
    fun us6_1_searching_without_fix() =
        runTest(dispatcher) {
            watch()
            fix(accuracy = Double.NaN)
            assertEquals(MeasureUiState.Searching, viewModel.uiState.value)
        }

    @Test
    fun us6_1_countdown_from_first_fix() =
        runTest(dispatcher) {
            watch()
            advanceTimeBy(5_000)
            fix(accuracy = 6.0)
            satellites.emit(listOf(gps(isUsed = true), gps(isUsed = false)))
            advanceTimeBy(12_001)
            val state = viewModel.uiState.value as MeasureUiState.Measuring
            assertEquals(18, state.remainingSeconds)
            assertEquals(6.0, state.average.accuracyMeters, PRECISION)
            assertEquals(mapOf(GnssSystem.GPS to 1), state.satellites)
        }

    /** Po 30 s wynik zostaje, a odbiornik przestaje nasłuchiwać. Wymagania: NFR-27 */
    @Test
    fun us6_1_finished_stops_location() =
        runTest(dispatcher) {
            watch()
            fix(accuracy = 6.0)
            advanceTimeBy(10_000)
            fix(accuracy = 6.0)
            advanceTimeBy(21_000)
            val state = viewModel.uiState.value as MeasureUiState.Finished
            assertEquals(2, state.average.fixCount)
            assertEquals(listOf(0, 0), listOf(fixes.subscriptionCount.value, satellites.subscriptionCount.value))
        }

    /** Wynik pomiaru zostaje po powrocie na ekran, np. z C2. Wymagania: US6.1 */
    @Test
    fun us6_1_result_kept_after_leave() =
        runTest(dispatcher) {
            val watching = watch()
            fix(accuracy = 6.0)
            advanceTimeBy(31_000)
            watching.cancel()
            advanceTimeBy(60_000)
            watch()
            assertEquals(MeasureUiState.Finished::class, viewModel.uiState.value::class)
        }

    /**
     * „Powtórz pomiar” po 10 s uśredniania zaczyna je od nowa: 29 s po nowym odczycie pomiar trwa, po 31 s jest
     * zakończony.
     * Scenariusz: pomiar-precyzyjny.feature: Uśrednianie można przerwać i zacząć od nowa
     *
     * Wymagania: US6.1
     */
    @Test
    fun us6_1_repeat_while_averaging() =
        runTest(dispatcher) {
            watch()
            fix(accuracy = 6.0)
            advanceTimeBy(10_000)
            assertEquals(MeasureUiState.Measuring::class, viewModel.uiState.value::class)

            viewModel.repeat()
            runCurrent()
            assertEquals(MeasureUiState.Searching, viewModel.uiState.value)
            fix(accuracy = 6.0)
            advanceTimeBy(29_000)
            assertEquals(MeasureUiState.Measuring::class, viewModel.uiState.value::class)
            advanceTimeBy(2_000)
            assertEquals(MeasureUiState.Finished::class, viewModel.uiState.value::class)
        }

    /** „Powtórz pomiar” zaczyna od szukania satelitów, bez limitu powtórzeń. */
    @Test
    fun nfr23_repeat_starts_over() =
        runTest(dispatcher) {
            watch()
            fix(accuracy = 6.0)
            advanceTimeBy(31_000)
            viewModel.repeat()
            runCurrent()
            assertEquals(MeasureUiState.Searching, viewModel.uiState.value)
            assertEquals(1, fixes.subscriptionCount.value)

            fix(accuracy = 6.0)
            advanceTimeBy(31_000)
            assertEquals("A-1", (place() as PlaceChoice.Suggested).hint.building.symbol)
        }

    /** Błąd odbiornika: stan błędu bez awarii, a „Powtórz pomiar” zaczyna nowy pomiar. Wymagania: NFR-50, NFR-23 */
    @Test
    fun nfr50_source_error_repeat() =
        runTest(dispatcher) {
            failure = SecurityException("Brak zgody")
            watch()
            assertEquals(MeasureUiState.Failed, viewModel.uiState.value)
            assertEquals(0, satellites.subscriptionCount.value)

            failure = IllegalArgumentException("Brak GPS")
            viewModel.repeat()
            runCurrent()
            assertEquals(MeasureUiState.Failed, viewModel.uiState.value)

            failure = null
            viewModel.repeat()
            runCurrent()
            assertEquals(MeasureUiState.Searching, viewModel.uiState.value)
            fix(accuracy = 6.0)
            advanceTimeBy(31_000)
            assertEquals(MeasureUiState.Finished::class, viewModel.uiState.value::class)
        }

    /**
     * Wyłączona lokalizacja: stan bez nasłuchu odbiornika; po włączeniu pomiar rusza sam, bez zegara.
     *
     * Wymagania: UC7
     */
    @Test
    fun uc7_4c_location_off_then_on() =
        runTest(dispatcher) {
            locationOn.value = false
            watch()
            assertEquals(MeasureUiState.LocationOff, viewModel.uiState.value)
            assertEquals(listOf(0, 0), listOf(fixes.subscriptionCount.value, satellites.subscriptionCount.value))

            locationOn.value = true
            runCurrent()
            assertEquals(MeasureUiState.Searching, viewModel.uiState.value)
            fix(accuracy = 6.0)
            advanceTimeBy(31_000)
            assertEquals(MeasureUiState.Finished::class, viewModel.uiState.value::class)
        }

    /**
     * Ekran niewidoczny dłużej niż 5 s w trakcie pomiaru: pomiar od nowa z informacją o przerwie.
     *
     * Wymagania: NFR-23, NFR-27
     */
    @Test
    fun nfr27_restart_after_leave() =
        runTest(dispatcher) {
            val watching = watch()
            fix(accuracy = 6.0)
            advanceTimeBy(10_000)
            watching.cancel()
            advanceTimeBy(6_000)
            watch()
            assertEquals(MeasureUiState.Searching, viewModel.uiState.value)
            assertTrue(viewModel.restarted.value)

            viewModel.repeat()
            runCurrent()
            assertFalse(viewModel.restarted.value)
        }

    /** Krótka nieobecność (obrót ekranu) nie przerywa pomiaru. */
    @Test
    fun nfr27_short_leave_continues() =
        runTest(dispatcher) {
            val watching = watch()
            fix(accuracy = 6.0)
            advanceTimeBy(10_000)
            watching.cancel()
            advanceTimeBy(3_000)
            watch()
            assertEquals(MeasureUiState.Measuring::class, viewModel.uiState.value::class)
            assertFalse(viewModel.restarted.value)
        }

    /** Punkt pomiaru leży w konturze A-1, więc aplikacja podpowiada A-1. Wymagania: US6.2 */
    @Test
    fun us6_2_hint_after_measurement() =
        runTest(dispatcher) {
            finish()
            assertEquals("A-1", (place() as PlaceChoice.Suggested).hint.building.symbol)
        }

    @Test
    fun us6_2_finish_once_at_deadline() =
        runTest(dispatcher) {
            watch()
            fix(accuracy = 6.0)
            advanceTimeBy(30_000)
            assertTrue(fixes.tryEmit(PositionFix(A1_POINT, 6.0, clock.nowMillis())))
            assertTrue(satellites.tryEmit(listOf(gps(isUsed = true))))
            runCurrent()

            val finished = viewModel.uiState.value as MeasureUiState.Finished
            assertEquals(1, catalogReads)
            assertEquals(emptyList<Satellite>(), finished.sky)
            assertEquals("A-1", (finished.place as PlaceChoice.Suggested).hint.building.symbol)
            viewModel.choose("B-1")
            assertTrue(fixes.tryEmit(PositionFix(A1_POINT, 6.0, clock.nowMillis())))
            assertTrue(satellites.tryEmit(emptyList()))
            runCurrent()
            assertEquals("B-1", (place() as PlaceChoice.Chosen).building.symbol)
        }

    @Test
    fun us6_2_confirm_hint() =
        runTest(dispatcher) {
            finish()
            viewModel.confirm()
            runCurrent()
            assertEquals("A-1", (place() as PlaceChoice.Chosen).building.symbol)
        }

    @Test
    fun us6_2_choose_other_building() =
        runTest(dispatcher) {
            finish()
            viewModel.choose("B-1")
            runCurrent()
            assertEquals("B-1", (place() as PlaceChoice.Chosen).building.symbol)
        }

    /**
     * Sekcja „Wybierz budynek”: budynki najbliżej punktu, od najbliższego, ten z punktem pierwszy z 0 m.
     *
     * Wymagania: US6.2
     */
    @Test
    fun us6_2_nearest_for_section() =
        runTest(dispatcher) {
            finish()
            val nearest = viewModel.buildings.value
            assertEquals(listOf("A-1", "B-1"), nearest.map { it.building.symbol })
            assertEquals(0.0, nearest.first().distanceMeters, PRECISION)
            assertEquals(1, lookupDispatches)
        }

    @Test
    fun br_g2_leave_outside() =
        runTest(dispatcher) {
            finish()
            viewModel.outside()
            runCurrent()
            assertEquals(PlaceChoice.Outside, place())
        }

    /** Korekta ręczna po pomiarze zmienia punkt, a „Powtórz pomiar” ją kasuje. Wymagania: US6.4, NFR-3 */
    @Test
    fun us6_4_move_point_until_repeat() =
        runTest(dispatcher) {
            viewModel.movePoint(MOVED)
            finish()
            viewModel.movePoint(MOVED)
            runCurrent()
            val moved = (viewModel.uiState.value as MeasureUiState.Finished).point
            viewModel.repeat()
            finish()
            assertEquals(MOVED to A1_POINT, moved to (viewModel.uiState.value as MeasureUiState.Finished).point)
        }

    /** Dalej niż 50 m od konturu: bez podpowiedzi, od razu „poza budynkiem”. Wymagania: BR-G4 */
    @Test
    fun br_g4_far_point_outside() =
        runTest(dispatcher) {
            finish(GeoPoint(longitude = 17.2, latitude = 51.2))
            assertEquals(PlaceChoice.Outside, place())
        }

    /**
     * Wiersz: G5 — punkt na krawędzi konturu A-1: podpowiedź A-1 do
     * potwierdzenia, nie w konturze i bez przypisania budynku.
     * Scenariusz: budynki-bez-szczegolow.feature: Punkt dokładnie na krawędzi konturu jest poza budynkiem z propozycją
     *
     * Wymagania: US6.2
     */
    @Test
    fun us6_2_edge_suggests_not_inside() =
        runTest(dispatcher) {
            finish(A1_EDGE_POINT)
            val suggested = place() as PlaceChoice.Suggested
            assertEquals("A-1", suggested.hint.building.symbol)
            assertFalse(suggested.hint.isInside)
        }

    /**
     * 30 m na północ od konturu A-1: podpowiedź A-1 do potwierdzenia, punkt nie leży w konturze.
     * Scenariusz (częściowo): budynki-bez-szczegolow.feature: Propozycja budynku po pomiarze zależy od odległości od
     *   konturu
     *
     * Wymagania: BR-G4
     */
    @Test
    fun br_g4_30_m_suggests_a1() =
        runTest(dispatcher) {
            finish(northOfA1(meters = 30.0))
            val suggested = place() as PlaceChoice.Suggested
            assertEquals("A-1", suggested.hint.building.symbol)
            assertFalse(suggested.hint.isInside)
        }

    /**
     * Tuż poniżej progu 50 m od konturu A-1: nadal podpowiedź A-1 do potwierdzenia.
     * Scenariusz (częściowo): budynki-bez-szczegolow.feature: Propozycja budynku po pomiarze zależy od odległości od
     *   konturu
     *
     * Wymagania: BR-G4
     */
    @Test
    fun br_g4_49_m_suggests_a1() =
        runTest(dispatcher) {
            finish(northOfA1(meters = 49.0))
            val suggested = place() as PlaceChoice.Suggested
            assertEquals("A-1", suggested.hint.building.symbol)
            assertFalse(suggested.hint.isInside)
        }

    /**
     * 60 m od konturu A-1 i dalej od innych budynków: bez podpowiedzi, od razu „poza budynkiem”.
     * Scenariusz (częściowo): budynki-bez-szczegolow.feature: Propozycja budynku po pomiarze zależy od odległości od
     *   konturu
     *
     * Wymagania: BR-G4
     */
    @Test
    fun br_g4_60_m_outside() =
        runTest(dispatcher) {
            finish(northOfA1(meters = 60.0))
            assertEquals(PlaceChoice.Outside, place())
        }

    private fun northOfA1(meters: Double): GeoPoint =
        GeoPoint(longitude = A1_POINT.longitude, latitude = A1_NORTH_EDGE + meters / METERS_PER_DEGREE)

    private fun TestScope.finish(point: GeoPoint = A1_POINT) {
        watch()
        backgroundScope.launch { fixes.emit(PositionFix(point, 6.0, clock.nowMillis())) }
        runCurrent()
        advanceTimeBy(31_000)
        runCurrent()
    }

    private fun place(): PlaceChoice = (viewModel.uiState.value as MeasureUiState.Finished).place

    private fun TestScope.watch() = backgroundScope.launch { viewModel.uiState.collect {} }.also { runCurrent() }

    private suspend fun fix(accuracy: Double) {
        fixes.emit(PositionFix(GeoPoint(longitude = 17.06, latitude = 51.11), accuracy, clock.nowMillis()))
        dispatcher.scheduler.runCurrent()
    }

    private fun gps(isUsed: Boolean): Satellite =
        Satellite(system = GnssSystem.GPS, id = 1, azimuthDegrees = 0.0, elevationDegrees = 0.0, isUsedInFix = isUsed)

    private companion object {
        const val BOOT = 1_000_000L
        const val PRECISION = 1e-9
        val A1_POINT = GeoPoint(longitude = 17.0605, latitude = 51.1105)
        val MOVED = GeoPoint(longitude = 17.0606, latitude = 51.1106)
        val A1_EDGE_POINT = GeoPoint(longitude = 17.06, latitude = 51.1105)
        const val A1_NORTH_EDGE = 51.111
        const val METERS_PER_DEGREE = 111_195.0
        val CAMPUS =
            """{"type":"FeatureCollection","attribution":"© OpenStreetMap","features":[""" +
                feature("A-1", west = 17.06) +
                "," +
                feature("B-1", west = 17.07) +
                "]}"

        fun feature(symbol: String, west: Double): String =
            """{"type":"Feature","properties":{"kind":"building","symbols":["$symbol"],"name":"$symbol",""" +
                """"osm_id":"way/$symbol"},"geometry":{"type":"Polygon","coordinates":[[[$west,51.11],""" +
                """[${west + 0.001},51.11],[${west + 0.001},51.111],[$west,51.111],[$west,51.11]]]}}"""
    }
}
