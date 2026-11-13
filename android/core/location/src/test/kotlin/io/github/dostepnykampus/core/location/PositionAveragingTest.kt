/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.location

import io.github.dostepnykampus.core.model.GeoPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

/** Uśrednianie pozycji przez 30 s na odczytach testowych, nie z telefonu. Wymagania: US6.1, NFR-1, NFR-23 */
class PositionAveragingTest {
    private val start = PositionAveraging(startMillis = START)

    @Test
    fun us6_1_equal_fixes_midpoint() {
        val average =
            start
                .withFix(fix(longitude = 17.0, latitude = 51.0, accuracy = 6.0, at = 1_000))
                .withFix(fix(longitude = 17.002, latitude = 51.002, accuracy = 6.0, at = 2_000))
                .averaged()
        assertEquals(17.001, average.point.longitude, DEGREES)
        assertEquals(51.001, average.point.latitude, DEGREES)
        assertEquals(6.0 / sqrt(2.0), average.accuracyMeters, PRECISION)
        assertEquals(2, average.fixCount)
    }

    /** Waga odczytu to odwrotność kwadratu promienia ufności: odczyt 1 m waży 4 razy więcej niż 2 m. */
    @Test
    fun us6_1_weights_by_accuracy() {
        val average =
            start
                .withFix(fix(longitude = 17.0, latitude = 51.0, accuracy = 1.0, at = 1_000))
                .withFix(fix(longitude = 17.005, latitude = 51.005, accuracy = 2.0, at = 2_000))
                .averaged()
        assertEquals(17.001, average.point.longitude, DEGREES)
        assertEquals(51.001, average.point.latitude, DEGREES)
        assertEquals(1.0 / sqrt(1.25), average.accuracyMeters, PRECISION)
    }

    /** Ostatnia znana pozycja sprzed startu i odczyt po 30 s nie wchodzą do średniej. */
    @Test
    fun us6_1_only_fixes_in_window() {
        val average =
            start
                .withFix(fix(longitude = 10.0, latitude = 50.0, accuracy = 1.0, at = -1))
                .withFix(fix(longitude = 17.0, latitude = 51.0, accuracy = 6.0, at = 0))
                .withFix(fix(longitude = 10.0, latitude = 50.0, accuracy = 1.0, at = AVERAGING_MILLIS))
                .averaged()
        assertEquals(17.0, average.point.longitude, DEGREES)
        assertEquals(51.0, average.point.latitude, DEGREES)
        assertEquals(1, average.fixCount)
    }

    @Test
    fun us6_1_skips_unknown_accuracy() {
        val averaging =
            listOf(Double.NaN, Double.POSITIVE_INFINITY, 0.0, -3.0).fold(start) { state, accuracy ->
                state.withFix(fix(longitude = 17.0, latitude = 51.0, accuracy = accuracy, at = 1_000))
            }
        assertNull(averaging.average())
    }

    @Test
    fun us6_1_has_accuracy() {
        assertEquals(
            listOf(true, false, false, false, false),
            listOf(4.5, 0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY).map {
                fix(longitude = 17.0, latitude = 51.0, accuracy = it, at = 0).hasAccuracy
            },
        )
    }

    @Test
    fun us6_1_no_fix_no_average() {
        assertNull(start.average())
    }

    /** Napis „Pomiar: zostało 12 s” zaokrągla w górę, więc 0 s pojawia się dopiero na końcu. */
    @Test
    fun us6_1_countdown_seconds() {
        assertEquals(
            listOf(30, 30, 13, 1, 0, 0),
            listOf(0L, 1L, 17_500L, 29_999L, 30_000L, 45_000L).map { start.remainingSeconds(START + it) },
        )
    }

    @Test
    fun us6_1_finished_after_window() {
        assertFalse(start.isFinished(START + AVERAGING_MILLIS - 1))
        assertTrue(start.isFinished(START + AVERAGING_MILLIS))
    }

    /** „Powtórz pomiar” zaczyna nowe pełne 30 s bez odczytów poprzedniego pomiaru. */
    @Test
    fun nfr23_repeat_starts_over() {
        val first = start.withFix(fix(longitude = 17.0, latitude = 51.0, accuracy = 6.0, at = 1_000))
        val repeated = first.restart(startMillis = START + 40_000)
        assertNull(repeated.average())
        assertEquals(30, repeated.remainingSeconds(START + 40_000))
    }

    private fun PositionAveraging.averaged(): PositionAverage = checkNotNull(average()) { "Brak średniej" }

    private fun fix(longitude: Double, latitude: Double, accuracy: Double, at: Long): PositionFix =
        PositionFix(
            point = GeoPoint(longitude = longitude, latitude = latitude),
            accuracyMeters = accuracy,
            elapsedMillis = START + at,
        )

    private companion object {
        const val START = 1_000_000L
        const val PRECISION = 1e-9
        const val DEGREES = 1e-9
    }
}
