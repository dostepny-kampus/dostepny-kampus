/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import io.github.dostepnykampus.core.designsystem.marking.mapMarkingImage
import io.github.dostepnykampus.core.designsystem.marking.mapPlanSignImage
import io.github.dostepnykampus.core.designsystem.marking.mapSeverityImage
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.room.RoomCategory
import io.github.dostepnykampus.core.model.submission.Severity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Barwy znaków wejść, barier i planu na mapie z tokenów `mapa.kolory` w obu motywach: wnętrze kształtu, symbol
 * w nim, kontur `tusz` i obwódka `papier`. Punkty leżą na siatce znaku 24 × 24.
 *
 * Wymagania: NFR-21, NFR-16, US1.3, US2.1, US16.1
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MapSignColorsTest {
    private val themes = mapOf("jasny" to LightMapColors, "ciemny" to DarkMapColors)

    @Test
    fun colorsBarrierSignsFromTokens() {
        themes.forEach { (theme, colors) ->
            Severity.entries.forEach { severity ->
                val image = mapSeverityImage(severity = severity, sizePx = SIZE, colors = colors)
                val seen = SEVERITY_POINTS.getValue(severity).map { (x, y) -> image.pixelOnSignGrid(x, y) }
                val expected = severityTones(severity, colors) + listOf(colors.tusz, colors.papier)
                assertEquals("$theme $severity: wnętrze, symbol, kontur, obwódka", expected.map { it.toArgb() }, seen)
            }
        }
    }

    @Test
    fun colorsEntranceSignsFromTokens() {
        themes.forEach { (theme, colors) ->
            EntranceMarking.entries.forEach { marking ->
                val image = mapMarkingImage(marking = marking, sizePx = SIZE, colors = colors)
                val seen = MARKING_POINTS.getValue(marking).map { (x, y) -> image.pixelOnSignGrid(x, y) }
                val expected = markingTones(marking, colors) + listOf(colors.tusz, colors.papier)
                assertEquals("$theme $marking: wnętrze, symbol, kontur, obwódka", expected.map { it.toArgb() }, seen)
            }
        }
    }

    @Test
    fun colorsPlanSignsFromTokens() {
        themes.forEach { (theme, colors) ->
            PLAN_POINTS.forEach { (category, points) ->
                val image = mapPlanSignImage(category = category, sizePx = SIZE, colors = colors)
                val seen = points.map { (x, y) -> image.pixelOnSignGrid(x, y) }
                val expected = planTones(category, colors) + listOf(colors.tusz, colors.papier)
                assertEquals("$theme $category: wnętrze, symbol, kontur, obwódka", expected.map { it.toArgb() }, seen)
            }
        }
    }

    private fun planTones(category: RoomCategory, colors: MapColors): List<Color> =
        if (category == RoomCategory.STAIRCASE) {
            listOf(colors.papier, colors.tusz)
        } else {
            listOf(colors.tusz, colors.papier)
        }

    private fun severityTones(severity: Severity, colors: MapColors): List<Color> =
        when (severity) {
            Severity.HINDRANCE -> listOf(colors.znakUtrudnienie, colors.znakUtrudnienieSymbol)
            Severity.ONLY_WITH_HELP -> listOf(colors.znakTylkoZPomoca, colors.znakTylkoZPomocaSymbol)
            Severity.BLOCKING -> listOf(colors.znakBlokada, colors.znakBlokadaSymbol)
        }

    private fun markingTones(marking: EntranceMarking, colors: MapColors): List<Color> =
        when (marking) {
            EntranceMarking.ACCESSIBLE -> listOf(colors.znakDostepne, colors.znakDostepneSymbol)
            EntranceMarking.INACCESSIBLE -> listOf(colors.tusz, colors.papier)
            EntranceMarking.NO_DATA -> listOf(colors.papier, colors.tusz)
        }

    private fun Bitmap.pixelOnSignGrid(x: Float, y: Float): Int {
        val scale = width / (GRID + HALO)
        return getPixel(((x + HALO / 2) * scale).toInt(), ((y + HALO / 2) * scale).toInt())
    }

    private companion object {
        const val SIZE = 282

        /** Bok siatki, na której leżą kształty znaku. */
        const val GRID = 24f

        /** Szerokość obwódki znaku na mapie w jednostkach siatki; połowa wystaje poza kształt. */
        const val HALO = 4.2f

        /** Punkty wnętrza, symbolu, konturu i obwódki każdego znaku uciążliwości. */
        val SEVERITY_POINTS =
            mapOf(
                Severity.HINDRANCE to listOf(8f to 18f, 12f to 11.5f, 12f to 20.6f, 12f to 22f),
                Severity.ONLY_WITH_HELP to listOf(12f to 18f, 9f to 8.8f, 3.2f to 8f, 1.8f to 8f),
                Severity.BLOCKING to listOf(12f to 5f, 12f to 12f, 2f to 12f, 0.6f to 12f),
            )

        /** Punkty wnętrza, symbolu, konturu i obwódki każdego znaku wejścia. */
        val MARKING_POINTS =
            mapOf(
                EntranceMarking.ACCESSIBLE to listOf(12f to 4f, 10.3f to 15.7f, 12f to 1f, 12f to -0.4f),
                EntranceMarking.INACCESSIBLE to listOf(12f to 4f, 12f to 12f, 12f to 1.28f, 12f to -0.12f),
                EntranceMarking.NO_DATA to listOf(12f to 19f, 12f to 16.6f, 6.75f to 6.75f, 5.76f to 5.76f),
            )

        /** Punkty wnętrza, symbolu, konturu i obwódki znaków windy, toalety dostosowanej i schodów na planie. */
        val PLAN_POINTS =
            mapOf(
                RoomCategory.ELEVATOR to listOf(4f to 12f, 12f to 7.6f, 12f to 1f, 12f to -0.4f),
                RoomCategory.ACCESSIBLE_TOILET to listOf(4f to 12f, 10.5f to 6.1f, 12f to 1f, 12f to -0.4f),
                RoomCategory.STAIRCASE to listOf(4f to 12f, 7.2f to 17.9f, 12f to 1f, 12f to -0.4f),
            )
    }
}
