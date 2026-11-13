/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.location

import android.location.LocationManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper

/** Włącznik lokalizacji telefonu ze zdarzeń systemowych, bez zegara. Wymagania: NFR-23, NFR-27 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GnssLocationSwitchTest {
    private val app = RuntimeEnvironment.getApplication()
    private val manager = app.getSystemService(LocationManager::class.java)

    /** Wyłączona lokalizacja, potem włączona i znów wyłączona: każda zmiana raz, w kolejności zdarzeń. */
    @Test
    fun nfr23_switch_follows_events() =
        runTest {
            shadowOf(manager).setProviderEnabled(LocationManager.GPS_PROVIDER, false)
            val states = mutableListOf<Boolean>()
            val collecting =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    GnssLocationSwitch(app).isEnabled().collect { states += it }
                }
            toggle(isEnabled = true)
            toggle(isEnabled = true)
            toggle(isEnabled = false)
            collecting.cancel()
            assertEquals(listOf(false, true, false), states)
        }

    private fun toggle(isEnabled: Boolean) {
        shadowOf(manager).setProviderEnabled(LocationManager.GPS_PROVIDER, isEnabled)
        ShadowLooper.idleMainLooper()
    }
}
