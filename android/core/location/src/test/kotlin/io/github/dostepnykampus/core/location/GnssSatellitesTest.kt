/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.location

import android.Manifest
import android.location.GnssStatus
import android.location.LocationManager
import android.os.Looper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/** Stan satelitów z odbiornika GNSS telefonu w Robolectric (US6.1, US6.5, NFR-2, NFR-27). */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GnssSatellitesTest {
    private val app = RuntimeEnvironment.getApplication()
    private val manager = app.getSystemService(LocationManager::class.java)

    @Before
    fun grantLocation() {
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    @Test
    fun us6_1_status_to_satellites() =
        runTest {
            val updates = mutableListOf<List<Satellite>>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                GnssSatellites(app).satellites().collect { updates += it }
            }
            simulate(galileoInFix())
            val expected =
                Satellite(
                    system = GnssSystem.GALILEO,
                    id = 11,
                    azimuthDegrees = 120.0,
                    elevationDegrees = 35.0,
                    isUsedInFix = true,
                )
            assertEquals(listOf(listOf(expected)), updates)
        }

    // Napis „Satelity: GPS 7, Galileo 5, GLONASS 4”: tylko satelity w pozycji, systemy w stałej kolejności.
    @Test
    fun us6_1_counts_used_in_fix() {
        val satellites =
            listOf(
                satellite(GnssSystem.GLONASS, used = true),
                satellite(GnssSystem.GPS, used = true),
                satellite(GnssSystem.GALILEO, used = false),
                satellite(GnssSystem.GPS, used = true),
                satellite(GnssSystem.GALILEO, used = true),
            )
        assertEquals(
            listOf(GnssSystem.GPS to 2, GnssSystem.GALILEO to 1, GnssSystem.GLONASS to 1),
            satellites.countsInFix().toList(),
        )
    }

    @Test
    fun us6_1_constellations_mapped() {
        assertEquals(
            GnssSystem.entries,
            listOf(
                GnssStatus.CONSTELLATION_GPS,
                GnssStatus.CONSTELLATION_GALILEO,
                GnssStatus.CONSTELLATION_GLONASS,
                GnssStatus.CONSTELLATION_BEIDOU,
                GnssStatus.CONSTELLATION_QZSS,
                GnssStatus.CONSTELLATION_IRNSS,
                GnssStatus.CONSTELLATION_SBAS,
                GnssStatus.CONSTELLATION_UNKNOWN,
            ).map { gnssSystem(it) },
        )
    }

    // Lokalizacja tylko przy otwartym ekranie: po końcu zbierania stan satelitów już nie przychodzi.
    @Test
    fun nfr27_stop_ends_updates() =
        runTest {
            val updates = mutableListOf<List<Satellite>>()
            val collecting =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    GnssSatellites(app).satellites().collect { updates += it }
                }
            simulate(GnssStatus.Builder().build())
            collecting.cancel()
            simulate(GnssStatus.Builder().build())
            assertEquals(1, updates.size)
        }

    @Test
    fun nfr27_no_permission_fails() {
        shadowOf(app).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        assertThrows(SecurityException::class.java) { runBlocking { GnssSatellites(app).satellites().first() } }
    }

    private fun simulate(status: GnssStatus) {
        shadowOf(manager).simulateGnssStatus(status)
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun satellite(system: GnssSystem, used: Boolean): Satellite =
        Satellite(system = system, id = 1, azimuthDegrees = 0.0, elevationDegrees = 0.0, isUsedInFix = used)

    // Galileo 11: sygnał 40 dB-Hz, wysokość 35°, azymut 120°, użyty do pozycji.
    private fun galileoInFix(): GnssStatus {
        val builder = GnssStatus.Builder()
        builder.addSatellite(
            GnssStatus.CONSTELLATION_GALILEO,
            11,
            40f,
            35f,
            120f,
            false,
            false,
            true,
            false,
            0f,
            false,
            0f,
        )
        return builder.build()
    }
}
