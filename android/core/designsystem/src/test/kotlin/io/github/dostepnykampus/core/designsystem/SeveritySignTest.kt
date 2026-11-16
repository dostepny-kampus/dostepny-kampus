/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.marking.SeverityIcon
import io.github.dostepnykampus.core.designsystem.marking.nameRes
import io.github.dostepnykampus.core.designsystem.marking.mapSeverityImage
import io.github.dostepnykampus.core.model.submission.Severity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Znak uciążliwości bariery: trzy kształty, na mapie w obwódce i konturze. Wymagania: US1.3, NFR-21 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h240dp-xhdpi")
class SeveritySignTest {
    /**
     * Wiersz: V1, bariery jako znak uciążliwości: znaczenie niesie kształt, więc kontury trzech znaków na mapie się
     * różnią, a każdy znak ma kontur `tusz` i obwódkę `papier`.
     *
     * Wymagania: NFR-21
     */
    @Test
    fun drawsThreeShapes() {
        val images = Severity.entries.map { mapSeverityImage(severity = it, sizePx = SIZE, colors = LightMapColors) }
        val outlined = images.map { image -> image.pixels().map { it == LightMapColors.tusz.toArgb() } }

        assertEquals("kontury znaków uciążliwości się powtarzają", Severity.entries.size, outlined.toSet().size)
        images.forEach { image ->
            val frame = listOf(LightMapColors.tusz.toArgb(), LightMapColors.papier.toArgb())
            assertEquals("znak bez konturu albo obwódki", true, image.pixels().containsAll(frame))
        }
    }

    /**
     * „Blokada” to tablica z jedną poziomą belką: środkowy wiersz znaku ma belkę w barwie znaku na całej jej
     * szerokości, a nad belką, pod nią i po jej bokach jest tablica. Punkty na siatce znaku 24 × 24.
     *
     * Wymagania: NFR-21, US1.3
     */
    @Test
    fun drawsBlockingAsPlateWithBar() {
        val image = mapSeverityImage(severity = Severity.BLOCKING, sizePx = SIZE, colors = LightMapColors)
        val bar = listOf(6f, 9f, 12f, 15f, 18f).map { image.pixelOnSignGrid(x = it, y = 12f) }
        val around = listOf(12f to 5f, 12f to 19f, 3.5f to 12f, 20.5f to 12f)
        val plate = around.map { (x, y) -> image.pixelOnSignGrid(x = x, y = y) }

        val barColor = LightMapColors.znakBlokadaSymbol.toArgb()
        val plateColor = LightMapColors.znakBlokada.toArgb()
        assertEquals("belka „Blokady” nie jest pozioma na całej szerokości", List(bar.size) { barColor }, bar)
        assertEquals("tablica „Blokady” nie otacza belki", List(plate.size) { plateColor }, plate)
    }

    @Test
    fun drawsLightHifi() {
        captureRoboImage { Signs() }
    }

    @Test
    @Config(qualifiers = "+night")
    fun drawsDarkHifi() {
        captureRoboImage { Signs() }
    }

    @Composable
    private fun Signs() {
        DostepnyKampusTheme {
            Surface {
                Column(modifier = Modifier.padding(Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
                    Severity.entries.forEach { severity ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Odstepy.s),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SeverityIcon(severity)
                            Text(text = stringResource(severity.nameRes()), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }

    private fun Bitmap.pixelOnSignGrid(x: Float, y: Float): Int {
        val scale = width / (GRID + HALO)
        return getPixel(((x + HALO / 2) * scale).toInt(), ((y + HALO / 2) * scale).toInt())
    }

    private fun Bitmap.pixels(): List<Int> {
        val pixels = IntArray(width * height)
        getPixels(pixels, 0, width, 0, 0, width, height)
        return pixels.toList()
    }

    private companion object {
        const val SIZE = 282

        /** Bok siatki, na której leżą kształty znaku. */
        const val GRID = 24f

        /** Szerokość obwódki znaku na mapie w jednostkach siatki; połowa wystaje poza kształt. */
        const val HALO = 4.2f
    }
}
