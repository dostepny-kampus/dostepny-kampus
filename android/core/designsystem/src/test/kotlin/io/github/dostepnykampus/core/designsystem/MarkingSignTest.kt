/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import io.github.dostepnykampus.core.designsystem.marking.MARKING_SIGN_TAG
import io.github.dostepnykampus.core.designsystem.marking.MarkingSign
import io.github.dostepnykampus.core.designsystem.marking.mapMarkingImage
import io.github.dostepnykampus.core.model.building.EntranceMarking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

private const val OPAQUE = 0xFF

private val WORDS =
    mapOf(
        EntranceMarking.ACCESSIBLE to "Dostępne",
        EntranceMarking.INACCESSIBLE to "Niedostępne",
        EntranceMarking.NO_DATA to "Brak danych",
    )

/**
 * Oznaczenie wejścia w tekście: znak z mapy i legendy obok słowa zwykłym tekstem, znak bez nazwy dla TalkBack.
 *
 * Wymagania: NFR-21, US2.1, US3.3
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class MarkingSignTest {
    @get:Rule
    val compose = createComposeRule()

    private var colors: MapColors = LightMapColors

    private fun showAll() {
        compose.setContent {
            DostepnyKampusTheme {
                colors = DostepnyKampusTheme.mapColors
                Column { EntranceMarking.entries.forEach { MarkingSign(marking = it) } }
            }
        }
    }

    /**
     * Każde oznaczenie ma słowo ze słownika i ten sam obraz znaku co na mapie, w obu motywach, bez barwnego tła.
     * Scenariusz (częściowo): przegladanie-mapy.feature:
     *   Wejście ma oznaczenie znakiem z mapy i słowem, nie tylko kolorem
     */
    @Test
    fun drawsMapSignAndWord() {
        assertSignsAndWords()
    }

    @Test
    @Config(qualifiers = "+night")
    fun drawsMapSignAndWordInDarkTheme() {
        assertSignsAndWords()
    }

    /** TalkBack czyta słowo; znak jest ozdobą i nie ma własnej nazwy. */
    @Test
    fun leavesSignWithoutName() {
        showAll()
        val named = SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription)
        compose.onAllNodes(named, useUnmergedTree = true).assertCountEquals(0)
        WORDS.values.forEach { compose.onNodeWithText(it).assertExists() }
    }

    /** Własne słowo zastępuje słowo ze słownika, znak zostaje ten sam. */
    @Test
    fun showsOwnLabel() {
        compose.setContent {
            DostepnyKampusTheme { MarkingSign(marking = EntranceMarking.ACCESSIBLE, label = "Wejście dostępne") }
        }
        compose.onNodeWithText("Wejście dostępne").assertExists()
        compose.onNodeWithText("Dostępne").assertDoesNotExist()
        compose.onAllNodesWithTag(MARKING_SIGN_TAG, useUnmergedTree = true).assertCountEquals(1)
    }

    private fun assertSignsAndWords() {
        showAll()
        val signs = compose.onAllNodesWithTag(MARKING_SIGN_TAG, useUnmergedTree = true)
        signs.assertCountEquals(EntranceMarking.entries.size)
        EntranceMarking.entries.forEachIndexed { index, marking ->
            compose.onNodeWithText(WORDS.getValue(marking)).assertExists()
            val seen = signs[index].captureToImage().asAndroidBitmap()
            val expected = mapMarkingImage(marking = marking, sizePx = seen.width, colors = colors)
            assertSignInsideLikeMap(marking, expected, seen)
        }
    }

    private fun assertSignInsideLikeMap(marking: EntranceMarking, expected: Bitmap, seen: Bitmap) {
        assertEquals("$marking: wysokość znaku", expected.height, seen.height)
        var compared = 0
        for (y in 1 until expected.height - 1) {
            for (x in 1 until expected.width - 1) {
                if (expected.isInside(x, y)) {
                    val pixel = expected.getPixel(x, y)
                    val actual = seen.getPixel(x, y)
                    val message = "$marking: piksel ($x, $y) jak na mapie: ${hex(pixel)}, jest ${hex(actual)}"
                    assertTrue(message, sameColor(pixel, actual))
                    compared++
                }
            }
        }
        assertTrue("$marking: znak ma kryjące piksele", compared > 0)
    }
}

private const val CHANNEL_TOLERANCE = 8

private fun Bitmap.isInside(x: Int, y: Int): Boolean =
    (-1..1).all { dy -> (-1..1).all { dx -> getPixel(x + dx, y + dy) ushr 24 == OPAQUE } }

private val CHANNELS: List<(Int) -> Int> = listOf(Color::alpha, Color::red, Color::green, Color::blue)

private fun sameColor(a: Int, b: Int): Boolean =
    CHANNELS.all { channel -> abs(channel(a) - channel(b)) <= CHANNEL_TOLERANCE }

private fun hex(pixel: Int): String = "%08X".format(pixel)
