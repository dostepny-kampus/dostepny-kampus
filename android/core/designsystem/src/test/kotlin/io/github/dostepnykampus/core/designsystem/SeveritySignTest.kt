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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.marking.SeverityIcon
import io.github.dostepnykampus.core.designsystem.marking.nameRes
import io.github.dostepnykampus.core.designsystem.marking.severityImage
import io.github.dostepnykampus.core.model.submission.Severity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Znak uciążliwości bariery: trzy kształty w tych samych dwóch kolorach. Wymagania: US1.3, NFR-21 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h240dp-xhdpi")
class SeveritySignTest {
    /** Wiersz: V1, bariery jako znak uciążliwości, kształt, nie kolor. Wymagania: NFR-21 */
    @Test
    fun drawsThreeShapesSameColors() {
        val images =
            Severity.entries.map {
                severityImage(severity = it, sizePx = SIZE, shapeColor = INK, markColor = PAPER)
            }
        val inked = images.map { image -> image.pixels().map { it == INK.toArgb() } }

        assertEquals(Severity.entries.size, inked.toSet().size)
        images.forEach { image ->
            assertEquals(true, image.pixels().containsAll(listOf(INK.toArgb(), PAPER.toArgb())))
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
        val image = severityImage(severity = Severity.BLOCKING, sizePx = SIZE, shapeColor = INK, markColor = PAPER)
        val bar = listOf(6f, 9f, 12f, 15f, 18f).map { image.at(x = it, y = 12f) }
        val plate = listOf(12f to 5f, 12f to 19f, 3.5f to 12f, 20.5f to 12f).map { (x, y) -> image.at(x = x, y = y) }

        assertEquals("belka „Blokady” nie jest pozioma na całej szerokości", List(bar.size) { PAPER.toArgb() }, bar)
        assertEquals("tablica „Blokady” nie otacza belki", List(plate.size) { INK.toArgb() }, plate)
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

    /** Piksel obrazu znaku w punkcie ([x], [y]) siatki 24 × 24, z marginesem obwódki 1 jak w [severityImage]. */
    private fun Bitmap.at(x: Float, y: Float): Int {
        val scale = width / (GRID + 2 * HALO)
        return getPixel(((x + HALO) * scale).toInt(), ((y + HALO) * scale).toInt())
    }

    private fun Bitmap.pixels(): List<Int> {
        val pixels = IntArray(width * height)
        getPixels(pixels, 0, width, 0, 0, width, height)
        return pixels.toList()
    }

    private companion object {
        const val SIZE = 96
        const val GRID = 24f
        const val HALO = 1f
        val INK = Color(color = 0xFF10242C)
        val PAPER = Color(color = 0xFFFFFFFF)
    }
}
