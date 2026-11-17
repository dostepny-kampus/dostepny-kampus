/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import io.github.dostepnykampus.app.navigation.sheetCard
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.feature.map.building.CardMode
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

private val CardHeight = 120.dp
private val StepDrag = 80.dp
private val HeightStep = 48.dp
private const val MOVE_MILLIS = 16L
private const val SLOW_DP_PER_SECOND = 100
private const val QUICK_DP_PER_SECOND = 200
private const val HOLD_MILLIS = 100L
private const val MILLIS_PER_SECOND = 1000f
private val Starts =
    listOf(
        SheetHeight.Low to true,
        SheetHeight.Low to false,
        SheetHeight.Half to true,
        SheetHeight.Half to false,
        SheetHeight.Full to false,
    )

/** Tempo przeciągnięcia: prędkość ruchu w dp/s i czas, przez który palec stoi przed puszczeniem. */
internal enum class DragPace(val dpPerSecond: Int, val holdMillis: Long) {
    /** Puszczenie w ruchu, wolniej niż rzut. */
    Slow(dpPerSecond = SLOW_DP_PER_SECOND, holdMillis = 0L),

    /** Puszczenie w ruchu, szybciej niż próg rzutu. */
    Quick(dpPerSecond = QUICK_DP_PER_SECOND, holdMillis = 0L),

    /** Palec zatrzymuje się przed puszczeniem, więc prędkość przy puszczeniu to zero. */
    Stopped(dpPerSecond = SLOW_DP_PER_SECOND, holdMillis = HOLD_MILLIS),
}

/**
 * Arkusz V2 po przeciągnięciu uchwytu o 80 dp równymi krokami staje na sąsiedniej wysokości przy każdym tempie,
 * także gdy palec zatrzymał się przed puszczeniem, a z niskiej w dół się zamyka.
 *
 * Wymagania: US2.8, NFR-53
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
internal class SheetStepDragTest(
    private val from: SheetHeight,
    private val isUp: Boolean,
    private val pace: DragPace,
) {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** Wiersz: przeciągnięcie o 80 dp → sąsiednia wysokość w stronę ruchu, z niskiej w dół zamknięcie. */
    @Test
    fun settlesOnNeighbourHeight() {
        open()
        repeat(from.ordinal - SheetHeight.Low.ordinal) {
            compose.onNodeWithContentDescription(EXPAND_HANDLE).performClick()
            compose.waitForIdle()
        }
        drag()
        val target = (if (isUp) from.higher() else from.lower()) ?: SheetHeight.Closed
        val handle = compose.onNodeWithContentDescription(EXPAND_HANDLE)
        if (target == SheetHeight.Closed) {
            handle.assertDoesNotExist()
        } else {
            val top = handle.getUnclippedBoundsInRoot().top
            assertEquals("góra uchwytu: $from → $target, $pace", topOf(target).value, top.value, 1f)
        }
    }

    private fun open() {
        compose.setContent {
            AppShell(firstLaunch = false, roles = TestRoles(), onFirstHelpLeft = {}) { route ->
                if (route is MapRoute) {
                    Column(modifier = Modifier.fillMaxSize().testTag("V1")) {
                        Text(text = "A-1", modifier = Modifier.clickable { openBuilding(symbol = "A-1") })
                    }
                } else {
                    val height = if (sheetCard().mode == CardMode.Compact) CardHeight else 2000.dp
                    val scroll = Modifier.verticalScroll(rememberScrollState())
                    Column(modifier = scroll) { Text(text = "karta", modifier = Modifier.requiredHeight(height)) }
                }
            }
        }
        compose.onNodeWithText("A-1").performClick()
        compose.waitForIdle()
    }

    private fun drag() {
        val step = pace.dpPerSecond.dp * (MOVE_MILLIS / MILLIS_PER_SECOND)
        val steps = (StepDrag / step).toInt()
        val sign = if (isUp) -1f else 1f
        val handle = compose.onNodeWithContentDescription(EXPAND_HANDLE)
        val start = handle.getUnclippedBoundsInRoot().top
        handle.performTouchInput {
            down(center)
            repeat(steps) { moveBy(delta = Offset(x = 0f, y = sign * step.toPx()), delayMillis = MOVE_MILLIS) }
        }
        if (pace.holdMillis > 0L) {
            compose.waitForIdle()
            val moved = abs((handle.getUnclippedBoundsInRoot().top - start).value)
            assertTrue("karta za palcem przed puszczeniem: $moved dp", moved >= HeightStep.value)
        }
        handle.performTouchInput {
            advanceEventTime(pace.holdMillis)
            up()
        }
        compose.waitForIdle()
    }

    private fun topOf(height: SheetHeight): Dp {
        val map = compose.onNodeWithTag("V1").getUnclippedBoundsInRoot().height
        return when (height) {
            SheetHeight.Full -> 0.dp
            SheetHeight.Half -> map / 2
            SheetHeight.Low, SheetHeight.Closed -> map - CeleDotyku.minimum - CardHeight
        }
    }

    /** Przypadki: każda wysokość, obie strony, trzy tempa. */
    companion object {
        /** Wysokość startowa, kierunek w górę i tempo przeciągnięcia. */
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}, w górę: {1}, {2}")
        fun cases(): List<Array<Any>> =
            Starts.flatMap { (from, isUp) -> DragPace.entries.map { pace -> arrayOf<Any>(from, isUp, pace) } }
    }
}
