/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.activity.ComponentActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val HEADER = "Wejścia"
private const val NAME = "Gmach Testowy"
private const val SCROLLED_PX = 500f

/**
 * V2 Karta budynku: kreska między górą karty a pełnym opisem na każdej wysokości karty i w pełnym ekranie od razu,
 * pod nagłówkiem karty i nad początkiem pełnego opisu; linia pod nagłówkiem przy przewiniętej treści.
 *
 * Wymagania: US2.8, NFR-53
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class CardDescriptionLineTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Wiersz: V2, niska wysokość: pod kreską wystaje początek pełnego opisu.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Niska karta nad mapą mówi słowami o wejściach
     */
    @Test
    fun drawsLineOnLowCard() {
        open(CardMode.Compact)
        val start = compose.onNode(hasContentDescription(HEADER) and hasClickAction()).getUnclippedBoundsInRoot().top
        assertLineAbove(start)
    }

    /**
     * Wiersz: V2, połowa ekranu: na karcie pod kreską początek pełnego opisu.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Uchwyt zmienia wysokość karty bez przeciągania
     */
    @Test
    fun drawsLineOnHalfCard() {
        open(CardMode.Expanded)
        assertLineAbove(compose.onNodeWithText(HEADER).getUnclippedBoundsInRoot().top)
    }

    /** Wiersz: V2, karta przy dużej czcionce na cały ekran nad mapą: pod kreską zaczyna się pełny opis. */
    @Test
    fun drawsLineOnLargeCard() {
        open(CardMode.Large)
        assertLineAbove(compose.onNodeWithText(HEADER).getUnclippedBoundsInRoot().top)
    }

    /** Wiersz: V2, pełny ekran od razu: pod kreską zaczyna się pełny opis. */
    @Test
    fun drawsLineOnDirectCard() {
        open(CardMode.Direct)
        assertLineAbove(compose.onNodeWithText(HEADER).getUnclippedBoundsInRoot().top)
    }

    /** Wiersz: V2, przewinięta treść karty: pod nagłówkiem z „Zamknij” cienka linia; bez przewinięcia jej nie ma. */
    @Test
    fun drawsLineOnlyWhenScrolled() {
        val scroll = ScrollState(initial = 0)
        open(CardMode.Expanded, card = longCard(), scroll = scroll)
        compose.onAllNodesWithTag(SCROLL_LINE_TAG).assertCountEquals(0)
        compose.onNodeWithText("Wejście 8").performScrollTo()
        compose.waitForIdle()
        assertTrue("treść się nie przewinęła", scroll.value > 0)
        val line = compose.onNodeWithTag(SCROLL_LINE_TAG).getUnclippedBoundsInRoot()
        val name = compose.onNodeWithText(NAME).getUnclippedBoundsInRoot()
        assertTrue("linia pod nagłówkiem: od ${line.top}, nazwa do ${name.bottom}", line.top >= name.bottom)
    }

    /** Zrzut przewiniętej karty na połowie ekranu: linia pod nagłówkiem nad uciętym wierszem, motyw jasny. */
    @Test
    @Config(qualifiers = "w360dp-h800dp-xhdpi")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun matchesScrolledMockup() {
        val scroll = ScrollState(initial = 0)
        open(CardMode.Expanded, card = longCard(), scroll = scroll)
        compose.runOnIdle { scroll.dispatchRawDelta(SCROLLED_PX) }
        compose.onRoot().captureRoboImage()
    }

    private fun longCard(): BuildingCardUiState.Ready {
        val entrances =
            (1..8).map { number ->
                Entrance(
                    id = "pwr/$number",
                    buildingSymbol = "A-2",
                    name = "Wejście $number",
                    isMain = false,
                    type = null,
                    location = GeoPoint(longitude = 0.5, latitude = 0.5),
                    marking = EntranceMarking.ACCESSIBLE,
                )
            }
        return BuildingCardUiState.Ready(symbol = "A-2", name = NAME, entrances = orderedEntrances(entrances))
    }

    private fun assertLineAbove(start: Dp) {
        val line = compose.onNodeWithTag(DESCRIPTION_LINE_TAG).getUnclippedBoundsInRoot()
        val name = compose.onNodeWithText(NAME).getUnclippedBoundsInRoot()
        assertTrue("kreska pod nagłówkiem: od ${line.top}, nazwa do ${name.bottom}", line.top >= name.bottom)
        assertTrue("kreska nad pełnym opisem: do ${line.bottom}, opis od $start", line.bottom <= start)
    }

    private fun open(
        mode: CardMode,
        card: BuildingCardUiState = BuildingCardUiState.Ready(symbol = "A-2", name = NAME, entrances = emptyList()),
        scroll: ScrollState = ScrollState(initial = 0),
    ) {
        compose.setContent {
            DostepnyKampusTheme {
                BuildingCardContent(
                    symbol = "A-2",
                    mode = mode,
                    card = card,
                    isOnline = true,
                    onShowOnMap = {},
                    onBack = {},
                    onClose = {},
                    onReport = {},
                    onRetry = {},
                    scroll = scroll,
                )
            }
        }
        compose.waitForIdle()
    }
}
