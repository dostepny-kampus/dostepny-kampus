/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import io.github.dostepnykampus.core.location.ElapsedClock
import io.github.dostepnykampus.core.location.GnssSystem
import io.github.dostepnykampus.core.location.PositionFix
import io.github.dostepnykampus.core.location.PositionSource
import io.github.dostepnykampus.core.location.Satellite
import io.github.dostepnykampus.core.location.SatelliteSource
import io.github.dostepnykampus.core.model.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Pomiar na ekranie C1 na odczytach testowych w czasie wirtualnym, bez odbiornika GNSS (US6.1, NFR-23, NFR-27). */
@OptIn(ExperimentalCoroutinesApi::class)
class MeasureViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val fixes = MutableSharedFlow<PositionFix>()
    private val satellites = MutableSharedFlow<List<Satellite>>()
    private val clock = ElapsedClock { BOOT + dispatcher.scheduler.currentTime }
    private val viewModel by lazy {
        MeasureViewModel(PositionSource { fixes }, SatelliteSource { satellites }, clock)
    }

    @BeforeEach
    fun main() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun reset() {
        Dispatchers.resetMain()
    }

    // „Szukamy satelitów”, dopóki nie ma odczytu ze znaną dokładnością.
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

    // Po 30 s wynik zostaje, a odbiornik przestaje nasłuchiwać (NFR-27).
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

    // Wynik pomiaru zostaje po powrocie na ekran, np. z C2 (US6.1).
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

    // „Powtórz pomiar” zaczyna od szukania satelitów, bez limitu powtórzeń.
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
        }

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
    }
}
