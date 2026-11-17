/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
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
import java.util.Locale
import kotlin.math.abs

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

    private fun showAll() {
        compose.setContent {
            DostepnyKampusTheme {
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
        compose.setContent { DostepnyKampusTheme { SignsBesideMapImages() } }
        val signs = compose.onAllNodesWithTag(MARKING_SIGN_TAG, useUnmergedTree = true)
        signs.assertCountEquals(EntranceMarking.entries.size)
        val screen = compose.onRoot().captureToImage().asAndroidBitmap()
        EntranceMarking.entries.forEachIndexed { index, marking ->
            compose.onNodeWithText(WORDS.getValue(marking)).assertExists()
            val sign = signs[index].fetchSemanticsNode().boundsInRoot
            val map = compose.onNodeWithTag(REFERENCE_TAG + marking.name).fetchSemanticsNode().boundsInRoot
            assertEquals("$marking: wielkość znaku jak na mapie", map.size, sign.size)
            assertSameImage(marking = marking, screen = screen, sign = sign, map = map)
        }
    }

    private fun assertSameImage(marking: EntranceMarking, screen: Bitmap, sign: Rect, map: Rect) {
        for (y in 0 until sign.height.toInt()) {
            for (x in 0 until sign.width.toInt()) {
                val expected = screen.getPixel(map.left.toInt() + x, map.top.toInt() + y)
                val actual = screen.getPixel(sign.left.toInt() + x, sign.top.toInt() + y)
                val message = "$marking: piksel ($x, $y) jak na mapie: ${hex(expected)}, jest ${hex(actual)}"
                assertTrue(message, sameColor(expected = expected, actual = actual))
            }
        }
    }
}

@Composable
private fun SignsBesideMapImages() {
    val colors = DostepnyKampusTheme.mapColors
    val px = with(LocalDensity.current) { iconSizeLikeText().roundToPx() }
    Column {
        EntranceMarking.entries.forEach { marking ->
            Row {
                MarkingSign(marking = marking)
                val map =
                    remember(key1 = marking, key2 = px, key3 = colors) {
                        mapMarkingImage(marking = marking, sizePx = px, colors = colors)
                    }
                Image(
                    bitmap = map.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.testTag(REFERENCE_TAG + marking.name),
                )
            }
        }
    }
}

private const val REFERENCE_TAG = "wzor-"

private const val CHANNEL_TOLERANCE = 8

private val CHANNELS: List<(Int) -> Int> = listOf(Color::alpha, Color::red, Color::green, Color::blue)

private fun sameColor(expected: Int, actual: Int): Boolean =
    CHANNELS.all { channel -> abs(channel(expected) - channel(actual)) <= CHANNEL_TOLERANCE }

private fun hex(pixel: Int): String = String.format(Locale.ROOT, "%08X", pixel)
