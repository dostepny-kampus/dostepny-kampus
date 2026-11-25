/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.location

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Włącznik trybu pokazu wybiera źródła położenia w wersji debug. Wymagania: US6.1 */
@OptIn(TrybPokazu::class)
@RunWith(RobolectricTestRunner::class)
class VariantSourcesTest {
    private val app = RuntimeEnvironment.getApplication()

    /** Wyłącza tryb pokazu po każdym teście. */
    @After
    fun switchOff() {
        DemoMode.setEnabled(app, isEnabled = false)
    }

    /** Włącznik trybu pokazu zamienia odbiornik GNSS na trasę. */
    @Test
    fun switchPicksRoute() =
        runTest {
            assertTrue("bez trybu pokazu nie GPS", variantPositions(app) is GnssPositionFixes)
            DemoMode.setEnabled(app, isEnabled = true)
            assertTrue("w trybie pokazu GPS", variantPositions(app) !is GnssPositionFixes)
            assertTrue("w trybie pokazu satelity z odbiornika", variantSatellites(app) !is GnssSatellites)
            assertTrue("w trybie pokazu lokalizacja wyłączona", variantLocationSwitch(app).isEnabled().first())
        }
}
