/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.location.GnssSystem
import io.github.dostepnykampus.core.location.Satellite
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Mapa nieba C1: położenie satelitów i systemy rozróżnione kształtem i podpisem. Wymagania: US6.5, NFR-21 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class SkyMapTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun show(vararg satellites: Satellite) {
        compose.setContent { DostepnyKampusTheme { SkyMap(Sky(satellites.toList())) } }
    }

    /** Wiersz: GPS, Galileo i GLONASS da się odróżnić w odcieniach szarości. */
    @Test
    fun us6_5_systems_differ_by_shape() {
        val marks = listOf(GnssSystem.GPS, GnssSystem.GALILEO, GnssSystem.GLONASS, GnssSystem.BEIDOU).map { it.mark() }
        assertEquals(marks.size, marks.toSet().size)
    }

    @Test
    fun us6_5_legend_names_shapes() {
        show(satellite(GnssSystem.GPS), satellite(GnssSystem.GALILEO), satellite(GnssSystem.QZSS))
        compose.onNodeWithText("Mapa nieba").assertIsDisplayed()
        compose.onNodeWithText("GPS").assertIsDisplayed()
        compose.onNodeWithText("Galileo").assertIsDisplayed()
        compose.onNodeWithText("QZSS").assertIsDisplayed()
    }

    /** Wszystkie widoczne satelity, także nieużyte do pozycji, tekstem dla czytnika ekranu. Wymagania: NFR-18 */
    @Test
    fun us6_5_screen_reader_text() {
        show(satellite(GnssSystem.GPS), satellite(GnssSystem.GPS, isUsed = false), satellite(GnssSystem.GLONASS))
        compose.onNodeWithContentDescription("Mapa nieba, widoczne satelity: GPS 2, GLONASS 1").assertIsDisplayed()
    }

    @Test
    fun us6_5_empty_sky() {
        show()
        compose.onNodeWithContentDescription("Mapa nieba: nie widać jeszcze satelitów").assertIsDisplayed()
    }

    @Test
    fun us6_5_zenith_center_north_up() {
        assertEquals(0f, skyOffset(azimuthDegrees = 0.0, elevationDegrees = 90.0, radius = 100f).getDistance(), DELTA)
        assertEquals(-100f, skyOffset(azimuthDegrees = 0.0, elevationDegrees = 0.0, radius = 100f).y, DELTA)
        assertEquals(50f, skyOffset(azimuthDegrees = 90.0, elevationDegrees = 45.0, radius = 100f).x, DELTA)
    }

    private fun satellite(system: GnssSystem, isUsed: Boolean = true): Satellite =
        Satellite(system = system, id = 1, azimuthDegrees = 45.0, elevationDegrees = 30.0, isUsedInFix = isUsed)

    private companion object {
        const val DELTA = 1e-3f
    }
}
