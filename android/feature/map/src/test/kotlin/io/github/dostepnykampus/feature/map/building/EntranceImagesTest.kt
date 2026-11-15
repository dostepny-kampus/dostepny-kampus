/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.Ikony
import io.github.dostepnykampus.core.designsystem.MapColors
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.feature.map.style.entranceMarkImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Znaki wejść na mapie V1: „Brak danych” jest mniejszy i pusty w środku, pozostałe mają zwykłą wielkość.
 *
 * Wymagania: US1.4, US2.1, NFR-21
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EntranceImagesTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun drawsNoDataSmallerThanOtherMarks() {
        val images = entranceImages(colors = colors(), density = 1f)
        val sizes = EntranceMarking.entries.associateWith { images.getValue(entranceMarkImage(it)).width }
        assertEquals(
            "„Brak danych” na mapie nie ma wielkości cichego znaku, a pozostałe znaki zwykłej",
            mapOf(
                EntranceMarking.ACCESSIBLE to Ikony.znakMapy.value.toInt(),
                EntranceMarking.INACCESSIBLE to Ikony.znakMapy.value.toInt(),
                EntranceMarking.NO_DATA to Ikony.znakMapyBrakDanych.value.toInt(),
            ),
            sizes,
        )
    }

    @Test
    fun drawsNoDataHollow() {
        val colors = colors()
        val images = entranceImages(colors = colors, density = 1f)
        val quiet = images.getValue(entranceMarkImage(EntranceMarking.NO_DATA))
        val full = images.getValue(entranceMarkImage(EntranceMarking.INACCESSIBLE))
        assertEquals(
            "Wnętrze rombu „Brak danych” na mapie nie ma koloru papieru mapy",
            colors.papier.toArgb(),
            quiet.getPixel(quiet.width / INSIDE_PART, quiet.height / 2),
        )
        assertEquals(
            "Wnętrze znaku „Niedostępne” na mapie nie ma koloru tuszu mapy",
            colors.tusz.toArgb(),
            full.getPixel(full.width / INSIDE_PART, full.height / 2),
        )
    }

    private fun colors(): MapColors {
        var palette: MapColors? = null
        compose.setContent { DostepnyKampusTheme { palette = DostepnyKampusTheme.mapColors } }
        compose.waitForIdle()
        return checkNotNull(palette)
    }

    private companion object {
        /** Punkt w czwartej części szerokości: wewnątrz kształtu, poza znakiem w środku. */
        const val INSIDE_PART = 4
    }
}
