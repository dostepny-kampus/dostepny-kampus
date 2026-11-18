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
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeUp
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

private val CompactHeight = 120.dp
private val ShortHeight = 200.dp
private val MediumHeight = 500.dp
private const val SLOW_SWIPE_MILLIS = 2000L
private val STEP_DRAG = 80.dp
private val TINY_DRAG = 30.dp
private const val FAST_SWIPE_MILLIS = 60L
private const val PARTIAL_SWIPE_MILLIS = 200L
private const val SWIPE_PAIRS = 3
private val SHORT_SWIPE = 60.dp
private const val LONG_SWIPE_MILLIS = 300L
private const val PEEK = "Wejścia"

/**
 * Arkusz V2 z dwiema wysokościami: niska i rozwinięta, do góry ekranu albo do wysokości krótkiej treści; idzie za
 * palcem, staje na najbliższej wysokości albo w stronę rzutu, z niskiej w dół się zamyka, a uchwyt i czynności
 * TalkBack zmieniają wysokość bez przeciągania.
 *
 * Wymagania: US2, US2.8, NFR-17, NFR-18, NFR-53
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class SheetHeightsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var cover = 0.dp

    private fun open(expandedHeight: Dp = 2000.dp) {
        compose.setContent {
            AppShell(firstLaunch = false, roles = TestRoles(), onFirstHelpLeft = {}) { route ->
                if (route is MapRoute) {
                    cover = LocalSheetPadding.current.calculateBottomPadding()
                    Column(modifier = Modifier.fillMaxSize().testTag("V1")) {
                        listOf("A-1", "A-2").forEach { symbol ->
                            Text(text = symbol, modifier = Modifier.clickable { openBuilding(symbol = symbol) })
                        }
                    }
                } else {
                    val card = sheetCard()
                    val height = if (card.mode == CardMode.Compact) CompactHeight else expandedHeight
                    val scroll = Modifier.verticalScroll(rememberScrollState())
                    Column(modifier = scroll) {
                        Column(modifier = Modifier.requiredHeight(height)) {
                            Text(text = "${card.mode}")
                            Text(text = PEEK, modifier = Modifier.clickable(onClick = card.onExpand))
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("A-1").performClick()
        compose.waitForIdle()
    }

    private fun map(): Dp = compose.onNodeWithTag("V1").getUnclippedBoundsInRoot().height

    private fun handleTop(): Dp = compose.onNodeWithContentDescription(EXPAND_HANDLE).getUnclippedBoundsInRoot().top

    private fun low(): Dp = map() - CeleDotyku.minimum - CompactHeight

    private fun assertAt(expected: Dp, what: String) {
        assertEquals("góra uchwytu: $what", expected.value, handleTop().value, 1f)
    }

    /**
     * Wiersz: dotknięcie uchwytu na niskiej → od razu rozwinięta do góry ekranu, a znowu → niska.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Uchwyt zmienia wysokość karty bez przeciągania
     */
    @Test
    fun tapsHandleThroughHeights() {
        open()
        assertAt(low(), "niska po otwarciu")
        compose.onNodeWithText("Compact").assertExists()
        val handle = compose.onNodeWithContentDescription(EXPAND_HANDLE)
        handle.performClick()
        compose.waitForIdle()
        assertAt(0.dp, "cały ekran")
        compose.onNodeWithText("Expanded").assertExists("pełny opis na całym ekranie")
        handle.performClick()
        assertAt(low(), "znowu niska")
        compose.onNodeWithText("Compact").assertExists()
    }

    /**
     * Wiersz: dotknięcie wystającego nagłówka „Wejścia” na niskiej wysokości od razu rozwija kartę, jak dotknięcie
     *   uchwytu.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Wystający nagłówek „Wejścia” od razu rozwija kartę
     */
    @Test
    fun raisesToFullFromPeek() {
        open()
        compose.onNodeWithText(PEEK).performClick()
        compose.waitForIdle()
        assertAt(0.dp, "cały ekran po dotknięciu „Wejścia”")
        compose.onNodeWithText("Expanded").assertExists("pełny opis po rozwinięciu")
    }

    /**
     * Wiersz: karta, której rozwinięta treść mieści się w połowie ekranu, rozwija się tylko do wysokości treści,
     *   a nad nią zostaje mapa; uchwytem i nagłówkiem „Wejścia” tak samo.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Wystający nagłówek „Wejścia” od razu rozwija kartę
     */
    @Test
    fun fitsShortCardToContent() {
        open(expandedHeight = ShortHeight)
        val top = map() - CeleDotyku.minimum - ShortHeight
        compose.onNodeWithContentDescription(EXPAND_HANDLE).performClick()
        compose.waitForIdle()
        assertAt(top, "krótka karta do wysokości treści")
        compose.onNodeWithText("Expanded").assertExists("pełny opis krótkiej karty")
        compose.onNodeWithContentDescription(EXPAND_HANDLE).performClick()
        compose.waitForIdle()
        assertAt(low(), "znowu niska")
        compose.onNodeWithText(PEEK).performClick()
        compose.waitForIdle()
        assertAt(top, "krótka karta po dotknięciu „Wejścia”")
    }

    /** Wiersz: rozwinięta treść dłuższa niż połowa ekranu, a krótsza od niego → karta do góry ekranu, bez luki. */
    @Test
    fun fillsScreenForLongerCard() {
        open(expandedHeight = MediumHeight)
        compose.onNodeWithContentDescription(EXPAND_HANDLE).performClick()
        compose.waitForIdle()
        assertAt(0.dp, "cały ekran przy treści dłuższej niż połowa")
        val sheet = compose.onNodeWithText("Expanded").getUnclippedBoundsInRoot()
        assertTrue("treść od góry karty: $sheet", sheet.top < CeleDotyku.minimum * 2)
    }

    /** Wiersz: kamera mapy nie rusza się przy zmianie wysokości karty — zasłonięta część mapy zostaje ta z niskiej. */
    @Test
    fun keepsMapCoverAtEveryHeight() {
        open()
        val low = cover
        assertEquals("zasłonięta część pod niską kartą", (map() - handleTop()).value, low.value, 1f)
        repeat(2) {
            compose.onNodeWithContentDescription(EXPAND_HANDLE).performClick()
            assertEquals("zasłonięta część bez zmian po rozwinięciu", low.value, cover.value, 1f)
        }
    }

    /** Wiersz: dotknięcie innego budynku zastępuje treść karty, a karta zostaje na swojej wysokości. */
    @Test
    fun keepsHeightForOtherBuilding() {
        open()
        compose.onNodeWithContentDescription(EXPAND_HANDLE).performClick()
        compose.onNodeWithText("A-2").performClick()
        compose.waitForIdle()
        assertAt(0.dp, "rozwinięta po wyborze innego budynku")
        compose.onNode(sheetHandle(isExpanded = true)).assertExists("uchwyt rozwiniętej karty")
    }

    /** Wiersz: wolne przeciągnięcie uchwytu w dół za połowę niskiej karty → karta się zamyka. */
    @Test
    fun closesOnShortDragFromLow() {
        open()
        dragHandle(by = CompactHeight)
        compose.onNodeWithContentDescription(EXPAND_HANDLE).assertDoesNotExist()
    }

    /** Wiersz: wolne przeciągnięcie uchwytu o 30 dp → karta wraca na swoją wysokość. */
    @Test
    fun returnsOnTinyDrag() {
        open()
        dragHandle(by = -TINY_DRAG)
        assertAt(low(), "niska po krótkim przeciągnięciu w górę")
    }

    /** Wiersz: wolne przeciągnięcie uchwytu w dół o 80 dp z rozwiniętej → karta wraca do góry ekranu. */
    @Test
    fun keepsFullOnShortDrag() {
        open()
        compose.onNodeWithContentDescription(EXPAND_HANDLE).performClick()
        compose.waitForIdle()
        dragHandle(by = STEP_DRAG)
        assertAt(0.dp, "rozwinięta po krótkim przeciągnięciu w dół")
    }

    private fun dragHandle(by: Dp) {
        compose.onNodeWithContentDescription(EXPAND_HANDLE).performTouchInput {
            val end = Offset(x = center.x, y = center.y + by.toPx())
            swipe(start = center, end = end, durationMillis = SLOW_SWIPE_MILLIS)
        }
        compose.waitForIdle()
    }

    /** Wiersz: wolne przeciągnięcie uchwytu w górę za połowę drogi do góry i puszczenie → karta rozwinięta. */
    @Test
    fun snapsToNearestHeight() {
        open()
        val up = handleTop() / 2 + 40.dp
        compose.onNodeWithContentDescription(EXPAND_HANDLE).performTouchInput {
            swipe(
                start = center,
                end = Offset(x = center.x, y = center.y - up.toPx()),
                durationMillis = SLOW_SWIPE_MILLIS,
            )
        }
        compose.waitForIdle()
        assertAt(0.dp, "rozwinięta po puszczeniu")
    }

    /** Wiersz: długie szybkie przeciągnięcie uchwytu w górę z niskiej → karta rozwinięta do góry ekranu. */
    @Test
    fun raisesOnLongSwipeUp() {
        open()
        val toTop = handleTop()
        compose.onNodeWithContentDescription(EXPAND_HANDLE).performTouchInput {
            swipe(
                start = center,
                end = Offset(x = center.x, y = center.y - toTop.toPx()),
                durationMillis = LONG_SWIPE_MILLIS,
            )
        }
        compose.waitForIdle()
        assertAt(0.dp, "rozwinięta po jednym geście z niskiej")
    }

    /** Wiersz: długie szybkie przeciągnięcie uchwytu w dół z rozwiniętej → karta staje na niskiej, nie zamyka się. */
    @Test
    fun stopsAtLowOnLongSwipeDown() {
        open()
        compose.onNodeWithContentDescription(EXPAND_HANDLE).performClick()
        compose.waitForIdle()
        assertAt(0.dp, "cały ekran przed gestem")
        compose.onNodeWithContentDescription(EXPAND_HANDLE).performTouchInput {
            swipe(
                start = center,
                end = Offset(x = center.x, y = center.y + map().toPx()),
                durationMillis = LONG_SWIPE_MILLIS,
            )
        }
        compose.waitForIdle()
        assertAt(low(), "niska po jednym geście z rozwiniętej")
    }

    /** Wiersz: przeciągnięcie w dół z niskiej wysokości → karta się zamyka i zostaje sama mapa. */
    @Test
    fun closesOnDragDownFromLow() {
        open()
        compose.onNodeWithContentDescription(EXPAND_HANDLE).performTouchInput {
            swipe(
                start = center,
                end = Offset(x = center.x, y = center.y + map().toPx()),
                durationMillis = SLOW_SWIPE_MILLIS,
            )
        }
        compose.waitForIdle()
        compose.onNodeWithText("Compact").assertDoesNotExist()
        compose.onNodeWithContentDescription(EXPAND_HANDLE).assertDoesNotExist()
        assertEquals("mapa bez zasłonięcia", 0f, cover.value, 1f)
    }

    /** Wiersz: przesunięcie treści niskiej karty w górę rozwija kartę. */
    @Test
    fun expandsOnContentSwipe() {
        open()
        compose.onNodeWithText("Compact").performTouchInput { swipeUp() }
        compose.waitForIdle()
        assertTrue("karta wyżej niż niska: ${handleTop()}", handleTop() < low() - 1.dp)
        compose.onNodeWithText("Expanded").assertExists()
    }

    /** Wiersz: szybkie krótkie przeciągnięcie treści w dół z niskiej wysokości → karta się zamyka. */
    @Test
    fun closesOnFastContentSwipe() {
        open()
        compose.onNodeWithText("Compact").performTouchInput {
            swipe(
                start = center,
                end = Offset(x = center.x, y = center.y + SHORT_SWIPE.toPx()),
                durationMillis = FAST_SWIPE_MILLIS,
            )
        }
        compose.waitForIdle()
        compose.onNodeWithContentDescription(EXPAND_HANDLE).assertDoesNotExist()
        assertEquals("mapa bez zasłonięcia", 0f, cover.value, 1f)
    }

    /** Wiersz: seria szybkich częściowych przeciągnięć treści w górę i w dół → karta stoi na jednej z wysokości. */
    @Test
    fun restsAtHeightAfterSwipes() {
        open()
        val bottom = map()
        compose.onRoot().performTouchInput {
            fun at(fromBottom: Dp) = Offset(x = center.x, y = (bottom - fromBottom).toPx())
            repeat(SWIPE_PAIRS) {
                swipe(start = at(40.dp), end = at(190.dp), durationMillis = PARTIAL_SWIPE_MILLIS)
                swipe(start = at(150.dp), end = at(60.dp), durationMillis = PARTIAL_SWIPE_MILLIS)
            }
        }
        compose.waitForIdle()
        val top = handleTop()
        val heights = listOf(0.dp, low())
        assertTrue("karta na jednej z wysokości $heights: $top", heights.any { abs(it.value - top.value) <= 1f })
    }

    /** Wiersz: TalkBack ma na uchwycie niskiej karty czynność „Rozwiń”, a rozwiniętej „Zwiń”. */
    @Test
    fun offersExpandAndCollapseActions() {
        open()
        assertEquals("czynności na niskiej", listOf("Rozwiń"), actions())
        perform("Rozwiń")
        assertAt(0.dp, "rozwinięta po „Rozwiń”")
        assertEquals("czynności na rozwiniętej", listOf("Zwiń"), actions())
        perform("Zwiń")
        assertAt(low(), "niska po „Zwiń”")
    }

    private fun actions(): List<String> =
        compose
            .onNodeWithContentDescription(EXPAND_HANDLE)
            .fetchSemanticsNode()
            .config
            .getOrElse(SemanticsActions.CustomActions) { emptyList() }
            .map { it.label }

    private fun perform(label: String) {
        val node = compose.onNodeWithContentDescription(EXPAND_HANDLE).fetchSemanticsNode()
        val action = node.config[SemanticsActions.CustomActions].first { it.label == label }
        compose.runOnUiThread { action.action() }
        compose.waitForIdle()
    }
}
