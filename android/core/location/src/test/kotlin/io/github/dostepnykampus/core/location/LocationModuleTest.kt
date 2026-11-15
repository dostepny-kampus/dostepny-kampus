/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.location

import android.os.SystemClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

/** Źródła pomiaru C1 w grafie Hilt to odbiornik GNSS i zegar telefonu. Wymagania: US6.1 */
@RunWith(RobolectricTestRunner::class)
class LocationModuleTest {
    @Test
    fun takesSourcesFromGnss() {
        assertTrue(LocationModule.positions(app()) is GnssPositionFixes)
        assertTrue(LocationModule.satellites(app()) is GnssSatellites)
        assertTrue(LocationModule.locationSwitch(app()) is GnssLocationSwitch)
    }

    /** Zegar pomiaru to czas od startu telefonu, jak `elapsedRealtimeNanos` w odczytach. */
    @Test
    fun usesElapsedClock() {
        ShadowSystemClock.advanceBy(Duration.ofSeconds(42))
        assertEquals(SystemClock.elapsedRealtime(), LocationModule.clock().nowMillis())
    }

    private fun app() = RuntimeEnvironment.getApplication()
}
