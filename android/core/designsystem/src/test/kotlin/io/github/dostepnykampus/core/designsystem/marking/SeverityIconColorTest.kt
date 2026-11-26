/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.marking

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import io.github.dostepnykampus.core.designsystem.DarkMapColors
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.LightMapColors
import io.github.dostepnykampus.core.designsystem.MapColors
import io.github.dostepnykampus.core.model.submission.Severity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.math.roundToInt

private const val SIGN = "znak"

/** Różnica kanału w skali 0–255, którą natywne rysowanie może dać na różnych procesorach. */
private const val CHANNEL_TOLERANCE = 2
private const val CHANNEL_MAX = 255f

/**
 * Poza mapą znak uciążliwości ma barwę i kontur znaku z mapy bieżącego motywu.
 *
 * Wymagania: NFR-21, US2.1
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h240dp-xxhdpi")
class SeverityIconColorTest {
    @get:Rule
    val compose = createComposeRule()

    /** W jasnym motywie „Blokada” ma barwę tablicy i kontur z jasnej mapy. */
    @Test
    fun usesLightMapTones() {
        assertSignColors(isDark = false, colors = LightMapColors)
    }

    /** W ciemnym motywie „Blokada” ma barwę tablicy i kontur z ciemnej mapy. */
    @Test
    fun usesDarkMapTones() {
        assertSignColors(isDark = true, colors = DarkMapColors)
    }

    private fun assertSignColors(isDark: Boolean, colors: MapColors) {
        compose.setContent {
            DostepnyKampusTheme(darkTheme = isDark) {
                SeverityIcon(severity = Severity.BLOCKING, modifier = Modifier.testTag(SIGN))
            }
        }
        compose.waitForIdle()
        val map = compose.onNodeWithTag(SIGN).captureToImage().toPixelMap()
        val pixels = (0 until map.width).flatMap { x -> (0 until map.height).map { y -> map[x, y] } }
        assertNear("znak bez barwy tablicy „Blokady”", colors.znakBlokada, pixels)
        assertNear("znak bez konturu znaku mapy", colors.tusz, pixels)
    }

    private fun assertNear(message: String, expected: Color, pixels: List<Color>) {
        val nearest = pixels.minBy { distance(it, expected) }
        assertTrue(
            "$message: oczekiwano ${expected.hex()}, najbliższy piksel ${nearest.hex()}",
            distance(nearest, expected) <= CHANNEL_TOLERANCE,
        )
    }

    private fun distance(a: Color, b: Color): Int =
        listOf(a.red - b.red, a.green - b.green, a.blue - b.blue).maxOf { abs(it * CHANNEL_MAX).roundToInt() }

    private fun Color.hex(): String = Integer.toHexString(toArgb())
}
