/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.scenario

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performScrollTo
import io.github.dostepnykampus.core.designsystem.marking.mapMarkingImage
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import android.graphics.Color as AndroidColor

private const val SIGN_TAG = "znak-oznaczenia"
private const val CHANNEL_TOLERANCE = 8
private const val GRAY_GAP = 0.25f
private const val MIN_GRAY_PIXELS = 100
private val WORDS = mapOf(EntranceMarking.ACCESSIBLE to "Dostępne", EntranceMarking.INACCESSIBLE to "Niedostępne")

private data class RowSign(val marking: EntranceMarking, val image: Bitmap, val baseline: Int)

/**
 * Lista wejść karty budynku A-1: oznaczenie każdego wejścia jako znak z mapy i słowo, bez tła, na wierszu listy.
 *
 * Wymagania: US2.1, NFR-21
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class EntranceListSignTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val flow = CardToMap(compose, symbol = "A-1")

    private val entrances =
        listOf(
            entrance(name = "Wejście główne", marking = EntranceMarking.INACCESSIBLE).copy(isMain = true),
            entrance(name = "Wejście boczne", marking = EntranceMarking.ACCESSIBLE),
            entrance(name = "Od dziedzińca", marking = EntranceMarking.INACCESSIBLE),
            entrance(name = "Północne", marking = EntranceMarking.ACCESSIBLE),
            entrance(name = "Południowe", marking = EntranceMarking.INACCESSIBLE),
            entrance(name = "Zachodnie", marking = EntranceMarking.ACCESSIBLE),
        )

    /**
     * Scenariusz: przegladanie-mapy.feature: Wejście ma oznaczenie znakiem z mapy i słowem, nie tylko kolorem.
     */
    @Test
    fun marksEntrancesBySignAndWord() {
        flow.show(entrances)
        val rows = entrances.map { hasClickAction() and hasText(it.name.orEmpty()) }
        compose.waitUntil { rows.all { compose.onAllNodes(it).fetchSemanticsNodes().isNotEmpty() } }

        val signs = entrances.map { rowSign(it) }

        val main = compose.onNode(hasClickAction() and hasText("Wejście główne"))
        main.assert(hasText("Niedostępne"))
        signs.forEach { sign ->
            assertSameAsMapSign(sign)
            assertNoBackground(sign)
        }
        val accessible = signs.first { it.marking == EntranceMarking.ACCESSIBLE }
        val inaccessible = signs.first { it.marking == EntranceMarking.INACCESSIBLE }
        val differing = grays(accessible).zip(grays(inaccessible)).count { (a, b) -> abs(a - b) > GRAY_GAP }
        val message = "znaki „dostępne” i „niedostępne” w szarości różnią się: $differing px"
        assertTrue(message, differing >= MIN_GRAY_PIXELS)
    }

    private fun rowSign(entrance: Entrance): RowSign {
        val row = compose.onNode(hasClickAction() and hasText(entrance.name.orEmpty()))
        row.performScrollTo()
        val word = WORDS.getValue(entrance.marking)
        row.assert(hasText(word))
        val rowBox = row.fetchSemanticsNode().boundsInRoot
        val signBox = signBoxes().single { rowBox.contains(it.center) }
        val picture = row.captureToImage().asAndroidBitmap()
        val sign =
            Bitmap.createBitmap(
                picture,
                (signBox.left - rowBox.left).toInt(),
                (signBox.top - rowBox.top).toInt(),
                signBox.width.toInt(),
                signBox.height.toInt(),
            )
        return RowSign(marking = entrance.marking, image = sign, baseline = picture.getPixel(picture.width - 1, 0))
    }

    private fun assertSameAsMapSign(sign: RowSign) {
        val map = mapMarkingImage(sign.marking, sizePx = sign.image.width, colors = flow.mapColors)
        pixels(sign.image).forEach { (x, y) ->
            val expected = Color(map.getPixel(x, y)).compositeOver(Color(sign.baseline))
            val message = "${sign.marking}: piksel ($x, $y) jak na mapie, na tle wiersza"
            assertTrue(message, isClose(expected = expected.toArgb(), actual = sign.image.getPixel(x, y)))
        }
    }

    private fun assertNoBackground(sign: RowSign) {
        val right = sign.image.width - 1
        val bottom = sign.image.height - 1
        listOf(0 to 0, right to 0, 0 to bottom, right to bottom).forEach { (x, y) ->
            val message = "${sign.marking}: róg znaku ($x, $y) ma barwę tła wiersza"
            assertTrue(message, isClose(expected = sign.baseline, actual = sign.image.getPixel(x, y)))
        }
    }

    private fun entrance(name: String, marking: EntranceMarking) =
        serverEntrance(id = name, symbol = "A-1", name = name, marking = marking)

    private fun signBoxes(): List<Rect> =
        compose.onAllNodesWithTag(SIGN_TAG, useUnmergedTree = true).fetchSemanticsNodes().map { it.boundsInRoot }

    private fun pixels(image: Bitmap): List<Pair<Int, Int>> =
        (0 until image.height).flatMap { y -> (0 until image.width).map { x -> x to y } }

    private fun grays(sign: RowSign): List<Float> =
        pixels(sign.image).map { (x, y) -> Color(sign.image.getPixel(x, y)).luminance() }

    private fun isClose(expected: Int, actual: Int): Boolean =
        listOf(AndroidColor::alpha, AndroidColor::red, AndroidColor::green, AndroidColor::blue).all { channel ->
            abs(channel(expected) - channel(actual)) <= CHANNEL_TOLERANCE
        }
}
