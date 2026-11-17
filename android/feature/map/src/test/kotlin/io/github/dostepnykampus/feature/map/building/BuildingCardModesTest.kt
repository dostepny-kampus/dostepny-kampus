/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.coroutines.EmptyCoroutineContext

private const val NAME = "Gmach Główny"
private const val WIDE_PILL_SHARE = 0.75f
private const val CHECK = "Pokaż wejście na mapie"
private const val ANSWER = "Zalecane: „Główne”."
private const val SHOW = "Pokaż budynek na mapie"
private const val CLOSE = "Zamknij"
private val TOUCH_TARGET = 48.dp
private const val BACK = "Wstecz"
private const val ROW = "Wejście dostępne"
private const val REPORT = "Zgłoś barierę przy wejściu"
private const val SHOW_ENTRANCE = "Pokaż wejście na mapie"
private const val UNNAMED = "Wejście bez nazwy w danych"
private val ENTRANCES = listOf("Główne" to "node/1", "Schody" to "node/2", UNNAMED to "node/3")

private val inScroll = hasAnyAncestor(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))

/** Treść V2 Karta budynku: mała karta, pełny ekran rozwinięty i pełny ekran od razu. Wymagania: US2.8, US2.9 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class BuildingCardModesTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val mode = mutableStateOf(CardMode.Compact)
    private val symbol = mutableStateOf("A-1")
    private val calls = mutableListOf<String>()
    private val online = MutableStateFlow(true)

    /**
     * Wiersz: V2, niska karta: nagłówek z obrysowanym „Zamknij” po prawej, wiersz o wejściach z odpowiedzią czytany
     *   jako jeden tekst i „Pokaż wejście na mapie” jako pierwsza czynność treści; bez dolnego „Zamknij”, listy
     *   „Wejścia”, „Wstecz” i „Pokaż budynek na mapie”.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Niska karta nad mapą mówi słowami o wejściach
     *
     * Wymagania: US2.8, US3.2, NFR-53
     */
    @Test
    fun showsCompactCard() {
        card(CardMode.Compact)
        listOf("Wejścia", "Schody", BACK, SHOW, "Pokaż na mapie").forEach {
            compose.onNodeWithText(it).assertDoesNotExist()
        }
        compose.onAllNodes(hasText(CLOSE)).assertCountEquals(1)
        val close = compose.onNodeWithText(CLOSE).getUnclippedBoundsInRoot()
        val symbol = compose.onNodeWithText("A-1", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val check = compose.onNodeWithText(CHECK).getUnclippedBoundsInRoot()
        assertTrue("„Zamknij” w nagłówku obok symbolu: $close, symbol $symbol", close.top < symbol.bottom)
        assertTrue("„Zamknij” po prawej od symbolu: $close, symbol $symbol", close.left > symbol.right)
        assertTrue("„Zamknij” nad „Pokaż wejście…”: $close, $check", close.bottom < check.top)
        compose.onNode(hasText(ROW) and hasText(ANSWER)).assertExists("wiersz i odpowiedź jako jeden tekst")
        compose.onAllNodes(hasClickAction() and !hasText(CLOSE)).onFirst().assert(hasText(CHECK))
        assertOrder(NAME, ROW, CHECK)
    }

    /**
     * Wiersz: V2, „Zamknij” w nagłówku niskiej karty ma cel dotyku co najmniej 48 × 48 dp.
     *
     * Wymagania: NFR-17, US2.8
     */
    @Test
    fun givesCloseTouchTarget() {
        card(CardMode.Compact)
        val close = compose.onNode(hasText(CLOSE) and hasClickAction()).getUnclippedBoundsInRoot()
        assertTrue("„Zamknij” wysokie na 48 dp, a jest $close", close.bottom - close.top >= TOUCH_TARGET)
        assertTrue("„Zamknij” szerokie na 48 dp, a jest $close", close.right - close.left >= TOUCH_TARGET)
    }

    /**
     * Wiersz: V2 bez internetu: plakietka „Bez internetu” nad nagłówkiem, w tym samym miejscu na małej karcie
     *   i na pełnym ekranie, węższa niż karta.
     *
     * Wymagania: NFR-10, US2.8
     */
    @Test
    fun showsOfflinePillAboveName() {
        online.value = false
        card(CardMode.Compact)
        listOf(CardMode.Compact, CardMode.Expanded).forEach { mode ->
            show(mode)
            assertOrder("Bez internetu", NAME, CHECK)
            val pill = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
            val width = pill.getUnclippedBoundsInRoot().run { right - left }
            val card = compose.onRoot().getUnclippedBoundsInRoot().run { right - left }
            assertTrue("plakietka węższa niż karta: $width z $card", width < card * WIDE_PILL_SHARE)
        }
    }

    /**
     * Wiersz: V2, pełny ekran: „Pokaż wejście na mapie” w treści pod nagłówkiem, pierwsza czynność, bez
     *   przypiętego pasa, potem grupa „Wejścia” jak dziś; „Pokaż budynek na mapie” tylko od razu.
     *
     * Wymagania: US3.2, US2.9
     */
    @Test
    fun putsCheckFirstInContent() {
        card(CardMode.Direct)
        listOf(CardMode.Direct, CardMode.Expanded).forEach { fullScreen ->
            show(fullScreen)
            compose.onAllNodesWithText(CHECK).onFirst().assert(inScroll)
            assertOrder(NAME, CHECK, "Wejścia", "Główne", "Schody")
            val actions = hasClickAction() and !hasText(BACK) and !hasText(CLOSE)
            compose.onAllNodes(actions).onFirst().assert(hasText(CHECK))
            compose.onNodeWithText("Wejście bez nazwy w danych").assertExists()
            assertOrder(NAME, ROW, CHECK)
        }
        compose.onNodeWithText(SHOW).assertDoesNotExist()
    }

    /**
     * Wiersz: V2, połowa ekranu i cały ekran: nagłówek niskiej karty z „Zamknij” po prawej, pod nim cały opis
     *   budynku; bez paska „Wstecz”. Pełny ekran otwarty od razu ma pasek „Wstecz” bez „Zamknij”.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Karta budynku zamyka się na każdej wysokości
     *
     * Wymagania: US2.8, NFR-53
     */
    @Test
    fun keepsHeaderCloseWhenExpanded() {
        card(CardMode.Expanded)
        compose.onNodeWithText(BACK).assertDoesNotExist()
        compose.onAllNodes(hasText(CLOSE)).assertCountEquals(1)
        val close = compose.onNodeWithText(CLOSE).getUnclippedBoundsInRoot()
        val symbol = compose.onNodeWithText("A-1", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("„Zamknij” w nagłówku obok symbolu: $close, symbol $symbol", close.top < symbol.bottom)
        assertTrue("„Zamknij” po prawej od symbolu: $close, symbol $symbol", close.left > symbol.right)
        assertOrder(NAME, CHECK, "Wejścia")
        compose.onNodeWithText(CLOSE).performClick()
        assertEquals(listOf("close"), calls)
        show(CardMode.Direct)
        compose.onNodeWithText(BACK).assertIsDisplayed()
        compose.onNodeWithText(CLOSE).assertDoesNotExist()
    }

    /**
     * Wiersz: V2 przy skali czcionki co najmniej 1,5: w nagłówku „Zamknij”, bez paska „Wstecz”, a pod nazwą
     *   „Pokaż wejście na mapie” i „Pokaż budynek na mapie”.
     *
     * Wymagania: NFR-20, US2.9
     */
    @Test
    fun keepsHeaderCloseAtLargeFont() {
        card(CardMode.Large)
        compose.onNodeWithText(BACK).assertDoesNotExist()
        compose.onAllNodes(hasText(CLOSE)).assertCountEquals(1)
        compose.onAllNodes(hasText("A-1"), useUnmergedTree = true).assertCountEquals(1)
        assertOrder(NAME, CHECK, SHOW)
    }

    /**
     * Wiersz: V2, „Zamknij” → onClose, „Wstecz” → onBack, „Pokaż wejście na mapie” → onShowEntrance z wejściem
     *   zalecanym, „Pokaż budynek na mapie” → onShowOnMap.
     *
     * Wymagania: NFR-15, US2.8
     */
    @Test
    fun callsEachActionCallback() {
        card(CardMode.Compact)
        compose.onNodeWithText(CLOSE).performClick()
        compose.onNodeWithText(CHECK).performClick()
        show(CardMode.Direct)
        compose.onNodeWithText(BACK).performClick()
        compose.onNodeWithText(SHOW).performClick()
        assertEquals(listOf("close", "show node/1", "back", "map"), calls)
    }

    /**
     * Wiersz: V2 od razu przy czcionce 200 %: treść i czynności w przewijanej treści, nic przypiętego.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Z listy przy dużej czcionce opis budynku prowadzi do mapy
     *
     * Wymagania: NFR-20
     */
    @Test
    @Config(fontScale = 2f)
    fun scrollsAllAtLargeFont() {
        card(CardMode.Direct)
        listOf(NAME, CHECK, SHOW, "Wejścia").forEach { compose.onAllNodesWithText(it).onFirst().assert(inScroll) }
        assertOrder(CHECK, SHOW, "Wejścia")
        compose.onAllNodes(hasClickAction() and !hasText(BACK) and !inScroll).assertCountEquals(0)
    }

    private fun reportButton(name: String) =
        compose.onNode(hasClickAction() and hasContentDescription("$REPORT: »$name«"))

    /**
     * Wiersz: V2, pełny ekran (rozwinięty i od razu): przy każdym wejściu, nie tylko zalecanym, „Zgłoś barierę
     *   przy wejściu” pod opisem wejścia, w przewijanej treści; TalkBack czyta „Zgłoś barierę przy wejściu:
     *   »nazwa wejścia«”, także dla wejścia bez nazwy, WCAG 2.5.3.
     *
     * Wymagania: US4.6, NFR-18
     */
    @Test
    fun offersReportAtEachEntrance() {
        card(CardMode.Direct)
        listOf(CardMode.Direct, CardMode.Expanded).forEach { fullScreen ->
            show(fullScreen)
            compose.onAllNodes(hasText(REPORT)).assertCountEquals(ENTRANCES.size)
            ENTRANCES.forEach { (name, _) -> reportButton(name).assert(hasText(REPORT) and inScroll) }
            val tops =
                ENTRANCES.flatMap { (name, _) ->
                    listOf(compose.onNodeWithText(name), reportButton(name)).map { it.getUnclippedBoundsInRoot().top }
                }
            val isDescending = tops.zipWithNext().all { (up, down) -> up < down }
            assertTrue("wejście, jego czynność, następne wejście od góry, a są na $tops", isDescending)
        }
    }

    /**
     * Wiersz: V2, dotknięcie „Zgłoś barierę przy wejściu” przy wejściu → onReportEntrance z identyfikatorem
     *   dokładnie tego wejścia.
     *
     * Wymagania: US4.6
     */
    @Test
    fun passesEntranceToReport() {
        card(CardMode.Expanded)
        ENTRANCES.forEach { (name, _) -> reportButton(name).performScrollTo().performClick() }
        assertEquals(ENTRANCES.map { (_, id) -> "report $id" }, calls)
    }

    /**
     * Wiersz: V2, mała karta nie ma listy wejść, więc nie ma „Zgłoś barierę przy wejściu”; pełny ekran ma.
     *
     * Wymagania: US4.6
     */
    @Test
    fun hidesReportOnCompact() {
        card(CardMode.Direct)
        compose.onAllNodes(hasText(REPORT)).assertCountEquals(ENTRANCES.size)
        show(CardMode.Compact)
        compose.onAllNodes(hasText(REPORT)).assertCountEquals(0)
        compose.onAllNodes(hasContentDescription(REPORT, substring = true)).assertCountEquals(0)
    }

    /**
     * Wiersz: V2 pełny ekran: w grupie „Wejścia” „Pokaż wejście na mapie” tylko przy wejściu zalecanym, pod jego
     *   opisem, w przewijanej treści; TalkBack czyta „Pokaż wejście na mapie: »nazwa wejścia«”; dotknięcie
     *   → onShowEntrance z id tego wejścia.
     *
     * Wymagania: US3.3, US2.1, BR-25, NFR-18
     */
    @Test
    fun showsRecommendedOnly() {
        card(CardMode.Direct)
        listOf(CardMode.Direct, CardMode.Expanded).forEach { fullScreen ->
            show(fullScreen)
            compose.onAllNodes(hasText(SHOW_ENTRANCE)).assertCountEquals(2)
            val shown = compose.onAllNodes(hasClickAction() and hasContentDescription("$SHOW_ENTRANCE: »Główne«"))[1]
            shown.assert(hasText(SHOW_ENTRANCE) and inScroll)
            val tops = listOf(compose.onNodeWithText("Główne"), shown, compose.onNodeWithText("Schody"))
            val ys = tops.map { it.getUnclippedBoundsInRoot().top }
            assertTrue("„Główne”, czynność, „Schody” od góry, a są na $ys", ys.zipWithNext().all { (a, b) -> a < b })
        }
        compose.onAllNodes(hasContentDescription("$SHOW_ENTRANCE: »Główne«"))[1].performClick()
        assertEquals(listOf("show node/1"), calls)
    }

    /**
     * Wiersz: V2 pełny ekran budynku bez wejścia zalecanego (każde „Niedostępne”) → bez „Pokaż wejście na mapie”,
     *   a pod wierszem o wejściach zdanie „Jedyne wejście nie jest dostępne.”.
     *
     * Wymagania: US3.3, US3.1, BR-25
     */
    @Test
    fun hidesShowWithoutRecommended() {
        card(CardMode.Direct)
        compose.onAllNodes(hasText(SHOW_ENTRANCE)).assertCountEquals(2)
        symbol.value = "B-4"
        compose.waitUntil { compose.onAllNodes(hasText("Zamknięte")).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(hasText(SHOW_ENTRANCE)).assertCountEquals(0)
        compose.onNodeWithText("Jedyne wejście nie jest dostępne.").assertExists("zdanie o jedynym wejściu")
    }

    /**
     * Wiersz: V2, mała karta ma jedno „Pokaż wejście na mapie” pod wierszem o wejściach; pełny ekran ma też drugie
     *   przy wejściu zalecanym w grupie „Wejścia”.
     *
     * Wymagania: US3.3
     */
    @Test
    fun hidesShowOnCompact() {
        card(CardMode.Direct)
        compose.onAllNodes(hasText(SHOW_ENTRANCE)).assertCountEquals(2)
        show(CardMode.Compact)
        compose.onAllNodes(hasText(SHOW_ENTRANCE)).assertCountEquals(1)
        compose.onAllNodes(hasContentDescription("$SHOW_ENTRANCE: »Główne«")).assertCountEquals(1)
    }

    private fun card(start: CardMode) {
        mode.value = start
        compose.setContent {
            val shown = symbol.value
            val viewModel =
                remember(shown) {
                    BuildingCardViewModel(symbol = shown, repository = repository, networkStatus = { online })
                }
            DostepnyKampusTheme {
                BuildingCardScreen(
                    symbol = shown,
                    mode = mode.value,
                    onShowOnMap = { calls += "map" },
                    onBack = { calls += "back" },
                    onClose = { calls += "close" },
                    onReportEntrance = { calls += "report $it" },
                    onShowEntrance = { calls += "show $it" },
                    viewModel = viewModel,
                )
            }
        }
        compose.waitUntil { compose.onAllNodes(hasText(NAME)).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun show(next: CardMode) {
        mode.value = next
        compose.waitForIdle()
    }

    private fun assertOrder(vararg texts: String) {
        val tops =
            texts.map { text ->
                compose
                    .onAllNodesWithText(text)
                    .onFirst()
                    .getUnclippedBoundsInRoot()
                    .top
            }
        assertTrue("${texts.toList()} od góry, a są na $tops", tops.zipWithNext().all { (up, down) -> up < down })
    }

    private companion object {
        val repository =
            BuildingCatalogRepository(EmptyCoroutineContext) {
                listOf(
                    """{"type":"Feature","properties":{"kind":"building","symbols":["A-1"],"display_name":"$NAME",""" +
                        """"osm_id":"way/1"},""" +
                        """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}""",
                    entrance(id = "node/1", tags = """"entrance":"main","wheelchair":"yes","ref":"Główne""""),
                    entrance(id = "node/2", tags = """"entrance":"yes","wheelchair":"no","ref":"Schody""""),
                    entrance(id = "node/3", tags = """"entrance":"yes","wheelchair":"no""""),
                    """{"type":"Feature","properties":{"kind":"building","symbols":["B-4"],""" +
                        """"display_name":"Budynek B","osm_id":"way/4"},""" +
                        """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}""",
                    entrance(id = "node/4", tags = """"wheelchair":"no","ref":"Zamknięte"""", symbol = "B-4"),
                ).joinToString(
                    separator = ",",
                    prefix = """{"type":"FeatureCollection","attribution":"©","features":[""",
                    postfix = "]}",
                )
            }

        fun entrance(id: String, tags: String, symbol: String = "A-1"): String =
            """{"type":"Feature","properties":{"kind":"entrance","symbols":["$symbol"],"osm_id":"$id",$tags},""" +
                """"geometry":{"type":"Point","coordinates":[0,0]}}"""
    }
}
