/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.marking

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
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

private const val CHANNEL_TOLERANCE_0_255 = 2
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
    val compose = createAndroidComposeRule<ComponentActivity>()

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
                SeverityIcon(severity = Severity.BLOCKING)
            }
        }
        compose.waitForIdle()
        val pixels = signPixels()
        assertNear("znak bez barwy tablicy „Blokady”", colors.znakBlokada, pixels)
        assertNear("znak bez konturu znaku mapy", colors.tusz, pixels)
    }

    private fun signPixels(): List<Color> {
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        compose.runOnIdle { view.draw(Canvas(bitmap)) }
        val raw = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(raw, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return raw.distinct().map(::Color)
    }

    private fun assertNear(message: String, expected: Color, pixels: List<Color>) {
        val nearest = pixels.minBy { distance(a = it, b = expected) }
        assertTrue(
            "$message: oczekiwano ${expected.hex()}, najbliższy piksel ${nearest.hex()}",
            distance(a = nearest, b = expected) <= CHANNEL_TOLERANCE_0_255,
        )
    }

    private fun distance(a: Color, b: Color): Int =
        listOf(a.red - b.red, a.green - b.green, a.blue - b.blue).maxOf { abs(it * CHANNEL_MAX).roundToInt() }

    private fun Color.hex(): String = Integer.toHexString(toArgb())
}
