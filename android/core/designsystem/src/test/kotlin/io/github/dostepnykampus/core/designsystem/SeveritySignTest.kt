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
import io.github.dostepnykampus.core.model.Severity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Znak uciążliwości bariery: trzy kształty w tych samych dwóch kolorach (US1.3, NFR-21). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h240dp-xhdpi")
class SeveritySignTest {
    /** Wiersz: V1, bariery jako znak uciążliwości, kształt, nie kolor (NFR-21). */
    @Test
    fun nfr21_three_shapes_same_colors() {
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

    @Test
    fun nfr21_hifi_motyw_jasny() {
        captureRoboImage { Signs() }
    }

    @Test
    fun nfr21_hifi_motyw_ciemny() {
        RuntimeEnvironment.setQualifiers("+night")
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

    private fun Bitmap.pixels(): List<Int> {
        val pixels = IntArray(width * height)
        getPixels(pixels, 0, width, 0, 0, width, height)
        return pixels.toList()
    }

    private companion object {
        const val SIZE = 96
        val INK = Color(color = 0xFF10242C)
        val PAPER = Color(color = 0xFFFFFFFF)
    }
}
