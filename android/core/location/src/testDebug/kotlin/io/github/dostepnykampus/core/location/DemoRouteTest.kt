/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.location

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Tryb pokazu: trasa z zasobów debug daje pełny pomiar C1 i satelity kilku systemów. Wymagania: US6.1, NFR-1 */
@OptIn(TrybPokazu::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DemoRouteTest {
    private val app = RuntimeEnvironment.getApplication()
    private val route = DemoRoute.load(app)

    /** Błąd pozycji spada od wartości przy fasadzie do poniżej metra w oknie pomiaru. */
    @Test
    fun errorDropsBelowMeter() {
        assertTrue("trasa krótsza niż pomiar", route.fixes.last().offsetMillis >= MEASURE_MILLIS)
        assertTrue("błąd na starcie nie jak przy fasadzie", route.fixes.first().accuracyMeters > FACADE_METERS)
        assertTrue("błąd na końcu nie poniżej metra", route.fixes.last().accuracyMeters < 1.0)
    }

    /** Satelity czterech systemów dochodzą do pozycji w czasie pomiaru. */
    @Test
    fun satellitesJoinFix() {
        val early = route.visibleAt(offsetMillis = 0).countsInFix()
        val late = route.visibleAt(offsetMillis = MEASURE_MILLIS).countsInFix()
        assertEquals("systemy na końcu pomiaru", SYSTEMS, late.keys)
        assertTrue("satelity nie dochodzą do pozycji", early.values.sum() < late.values.sum())
    }

    /** Uśrednianie z trasy kończy się po 30 s z błędem poniżej metra. */
    @Test
    fun averagingFinishesOnRoute() =
        runTest {
            val clock = ElapsedClock { testScheduler.currentTime }
            val fixes =
                route
                    .positions(clock)
                    .fixes()
                    .take(route.fixes.size + 1)
                    .toList()
            val averaging = fixes.fold(PositionAveraging(fixes.first().elapsedMillis)) { sum, fix -> sum.withFix(fix) }
            val average = checkNotNull(averaging.average()) { "brak średniej z trasy" }
            assertTrue("pomiar nie skończył się po 30 s", averaging.isFinished(fixes.last().elapsedMillis))
            assertEquals("odczyty w oknie 30 s", MEASURE_MILLIS.toInt() / TICK, average.fixCount)
            assertTrue("średnia z błędem ponad metr", average.accuracyMeters < 1.0)
        }

    private companion object {
        const val MEASURE_MILLIS = 30_000L
        const val TICK = 1_000
        const val FACADE_METERS = 5.0
        val SYSTEMS = setOf(GnssSystem.GPS, GnssSystem.GALILEO, GnssSystem.GLONASS, GnssSystem.BEIDOU)
    }
}
