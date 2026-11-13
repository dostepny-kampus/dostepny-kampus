/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.location

import android.Manifest
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import io.github.dostepnykampus.core.model.GeoPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/** Odczyty z odbiornika GNSS telefonu w Robolectric. Wymagania: US6.1, NFR-2, NFR-27 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GnssPositionFixesTest {
    private val app = RuntimeEnvironment.getApplication()
    private val manager = app.getSystemService(LocationManager::class.java)

    @Before
    fun grantLocation() {
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        shadowOf(manager).setProviderEnabled(LocationManager.GPS_PROVIDER, true)
    }

    @Test
    fun us6_1_gps_location_to_fix() =
        runTest {
            val fixes = mutableListOf<PositionFix>()
            val collecting =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    GnssPositionFixes(app).fixes().collect { fixes += it }
                }
            simulate(location(accuracy = 4.5f))
            collecting.cancel()
            val expected = PositionFix(GeoPoint(longitude = 17.06, latitude = 51.11), 4.5, elapsedMillis = 12_345)
            assertEquals(listOf(expected), fixes)
        }

    /** Odczyt bez promienia ufności dostaje NaN, więc uśrednianie go pomija. */
    @Test
    fun us6_1_no_accuracy_is_nan() =
        runTest {
            val fixes = mutableListOf<PositionFix>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                GnssPositionFixes(app).fixes().collect { fixes += it }
            }
            simulate(location(accuracy = null))
            assertTrue(fixes.single().accuracyMeters.isNaN())
        }

    /** Lokalizacja tylko przy otwartym ekranie: koniec zbierania zdejmuje nasłuch odbiornika. */
    @Test
    fun nfr27_stop_removes_listener() =
        runTest {
            val collecting =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    GnssPositionFixes(app).fixes().collect {}
                }
            val requests = shadowOf(manager).getLocationRequests(LocationManager.GPS_PROVIDER).size
            collecting.cancel()
            assertEquals(
                listOf(1, 0),
                listOf(requests, shadowOf(manager).getLocationRequests(LocationManager.GPS_PROVIDER).size),
            )
        }

    /** Bez zgody na lokalizację odbiornik nie rusza, a ekran dostaje błąd, żeby poprosić o zgodę. */
    @Test
    fun nfr27_no_permission_fails() {
        shadowOf(app).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        assertThrows(SecurityException::class.java) { runBlocking { GnssPositionFixes(app).fixes().first() } }
        assertTrue(shadowOf(manager).getLocationRequests(LocationManager.GPS_PROVIDER).isEmpty())
    }

    private fun simulate(location: Location) {
        shadowOf(manager).simulateLocation(LocationManager.GPS_PROVIDER, location)
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun location(accuracy: Float?): Location {
        val location = Location(LocationManager.GPS_PROVIDER)
        location.longitude = 17.06
        location.latitude = 51.11
        location.elapsedRealtimeNanos = 12_345_000_000L
        location.time = System.currentTimeMillis()
        if (accuracy != null) location.accuracy = accuracy
        return location
    }
}
