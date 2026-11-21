/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Kiedy górny pasek V1 odjeżdża nad zasłoniętą mapą. Wymagania: US18.1, US18.2, NFR-53 */
@RunWith(RobolectricTestRunner::class)
class MapTopBarTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** Wiersz: karta sali przy zwykłej czcionce chowa wiersz tytułu i pole, jak V2. */
    @Test
    fun roomCardCoversMap() {
        assertEquals("Karta sali nie schowała paska", true, covered(sheet = 0.dp, isRoomOpen = true, fontScale = 1f))
    }

    /** Wiersz: przy dużej czcionce karta sali stoi na cały ekran, więc mapy pod nią nie ma. */
    @Test
    fun largeRoomCardLeavesBar() {
        assertEquals("Duża czcionka schowała pasek", false, covered(sheet = 0.dp, isRoomOpen = true, fontScale = 2f))
    }

    /** Wiersz: arkusz V2 dalej chowa pasek. */
    @Test
    fun sheetCoversMap() {
        assertEquals("V2 nie schowała paska", true, covered(sheet = 120.dp, isRoomOpen = false, fontScale = 1f))
    }

    /** Wiersz: bez karty i arkusza pasek stoi. */
    @Test
    fun openMapKeepsBar() {
        assertEquals("Pasek zniknął bez karty", false, covered(sheet = 0.dp, isRoomOpen = false, fontScale = 1f))
    }

    private fun covered(sheet: Dp, isRoomOpen: Boolean, fontScale: Float): Boolean {
        var result: Boolean? = null
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density = density.density, fontScale = fontScale)) {
                result = isMapCovered(sheetPadding = sheet, isRoomOpen = isRoomOpen)
            }
        }
        compose.waitForIdle()
        return checkNotNull(result)
    }
}
