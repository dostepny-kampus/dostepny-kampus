/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
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
private const val CHECK = "Sprawdź, którędy wejść"
private const val SHOW = "Pokaż budynek na mapie"
private const val CLOSE = "Zamknij"
private const val BACK = "Wstecz"
private const val ROW = "Wejście dostępne"

private val inScroll = hasAnyAncestor(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))

/** Treść V2 Karta budynku: mała karta, pełny ekran rozwinięty i pełny ekran od razu (US2.8, US2.9). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class BuildingCardModesTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val mode = mutableStateOf(CardMode.Compact)
    private val calls = mutableListOf<String>()
    private val online = MutableStateFlow(true)

    // Wiersz: V2, mała karta: nagłówek, potem „Sprawdź, którędy wejść” jako pierwsza czynność i „Zamknij”; bez
    //   „Wejścia”, „Wstecz” i „Pokaż budynek na mapie”, US2.8, US3.2, decyzja 115.
    // Scenariusz (częściowo): przegladanie-mapy.feature: Mała karta nad mapą mówi słowami o wejściach
    @Test
    fun us2_8_compact_card() {
        card(CardMode.Compact)
        listOf("Wejścia", "Schody", BACK, SHOW, "Pokaż na mapie").forEach {
            compose.onNodeWithText(it).assertDoesNotExist()
        }
        assertOrder(NAME, CHECK, CLOSE)
        compose.onAllNodes(hasClickAction()).onFirst().assert(hasText(CHECK))
        assertOrder(NAME, ROW, CHECK)
    }

    // Wiersz: V2 bez internetu, mała karta: linia „Bez internetu” nad przyciskami, NFR-10, US2.8, decyzja 115.
    @Test
    fun nfr10_offline_above_compact() {
        online.value = false
        card(CardMode.Compact)
        assertOrder("Bez internetu", CHECK, CLOSE)
    }

    // Wiersz: V2, pełny ekran: „Sprawdź, którędy wejść” w treści pod nagłówkiem, pierwsza czynność, bez
    //   przypiętego pasa, potem grupa „Wejścia” jak dziś; „Pokaż budynek na mapie” tylko od razu, US3.2, US2.9.
    @Test
    fun us3_2_check_first_in_content() {
        card(CardMode.Direct)
        listOf(CardMode.Direct, CardMode.Expanded).forEach { fullScreen ->
            show(fullScreen)
            compose.onNodeWithText(CHECK).assert(inScroll)
            assertOrder(NAME, CHECK, "Wejścia", "Główne", "Schody")
            compose.onAllNodes(hasClickAction() and !hasText(BACK)).onFirst().assert(hasText(CHECK))
            compose.onNodeWithText(BACK).assertIsDisplayed()
            compose.onNodeWithText("Wejście bez nazwy w danych").assertExists()
            assertOrder(NAME, ROW, CHECK)
        }
        compose.onNodeWithText(SHOW).assertDoesNotExist()
    }

    // Wiersz: V2, „Zamknij” → onClose, „Wstecz” → onBack, „Sprawdź…” → onCheckEntrances, „Pokaż budynek na
    //   mapie” → onShowOnMap, NFR-15, US2.8.
    @Test
    fun nfr15_each_action_its_callback() {
        card(CardMode.Compact)
        compose.onNodeWithText(CLOSE).performClick()
        compose.onNodeWithText(CHECK).performClick()
        show(CardMode.Direct)
        compose.onNodeWithText(BACK).performClick()
        compose.onNodeWithText(SHOW).performClick()
        assertEquals(listOf("close", "check", "back", "map"), calls)
    }

    // Wiersz: V2 od razu przy czcionce 200 %: treść i czynności w przewijanej treści, nic przypiętego, NFR-20.
    // Scenariusz (częściowo): przegladanie-mapy.feature: Z listy przy dużej czcionce opis budynku prowadzi do mapy
    @Test
    @Config(fontScale = 2f)
    fun nfr20_large_font_all_scrolls() {
        card(CardMode.Direct)
        listOf(NAME, CHECK, SHOW, "Wejścia").forEach { compose.onNodeWithText(it).assert(inScroll) }
        assertOrder(CHECK, SHOW, "Wejścia")
        compose.onAllNodes(hasClickAction() and !hasText(BACK) and !inScroll).assertCountEquals(0)
    }

    private fun card(start: CardMode) {
        mode.value = start
        val viewModel = BuildingCardViewModel(symbol = "A-1", repository = repository, networkStatus = { online })
        compose.setContent {
            DostepnyKampusTheme {
                BuildingCardScreen(
                    symbol = "A-1",
                    mode = mode.value,
                    onCheckEntrances = { calls += "check" },
                    onShowOnMap = { calls += "map" },
                    onBack = { calls += "back" },
                    onClose = { calls += "close" },
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
        val tops = texts.map { compose.onNodeWithText(it).getUnclippedBoundsInRoot().top }
        assertTrue("${texts.toList()} od góry, a są na $tops", tops.zipWithNext().all { (up, down) -> up < down })
    }

    private companion object {
        val repository =
            BuildingCatalogRepository(EmptyCoroutineContext) {
                listOf(
                    """{"type":"Feature","properties":{"kind":"building","symbols":["A-1"],"name":"$NAME",""" +
                        """"osm_id":"way/1"},""" +
                        """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}""",
                    entrance(id = "node/1", tags = """"entrance":"main","wheelchair":"yes","ref":"Główne""""),
                    entrance(id = "node/2", tags = """"entrance":"yes","wheelchair":"no","ref":"Schody""""),
                    entrance(id = "node/3", tags = """"entrance":"yes","wheelchair":"no""""),
                ).joinToString(
                    separator = ",",
                    prefix = """{"type":"FeatureCollection","attribution":"©","features":[""",
                    postfix = "]}",
                )
            }

        fun entrance(id: String, tags: String): String =
            """{"type":"Feature","properties":{"kind":"entrance","symbols":["A-1"],"osm_id":"$id",$tags},""" +
                """"geometry":{"type":"Point","coordinates":[0,0]}}"""
    }
}
