/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.coroutines.EmptyCoroutineContext

/**
 * V2 Karta budynku: które wejście ma napis „Zalecane” i w jakiej kolejności stoją wejścia (BR-25, US2.1, US3.1).
 * Każdy test przechodzi cały scenariusz: dane wejść z katalogu, otwarta karta, lista wejść na ekranie.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class RecommendedEntrancePathTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** Scenariusz: przegladanie-mapy.feature: Wejście zalecane stoi pierwsze na karcie budynku. */
    @Test
    fun br25_side_recommended_first() {
        openCard(
            symbol = "A-1",
            entrance(name = "Główne", type = "main", wheelchair = "no"),
            entrance(name = "Boczne", wheelchair = "yes"),
            entrance(name = "Od dziedzińca", wheelchair = "no"),
            entrance(name = "Północne"),
            entrance(name = "Południowe"),
            entrance(name = "Zachodnie"),
        )
        assertEquals(
            listOf("Zalecane", "Boczne", "Główne", "Od dziedzińca", "Północne", "Południowe", "Zachodnie"),
            shown(NAMES),
        )
        assertEquals("Dostępne", shown(MARKINGS).first())
    }

    /** Scenariusz: przegladanie-mapy.feature: Przy kilku wejściach dostępnych zalecane jest wejście główne. */
    @Test
    fun br25_main_among_accessible() {
        openCard(
            symbol = "C-13",
            entrance(name = "Boczne", wheelchair = "yes"),
            entrance(name = "Główne", type = "main", wheelchair = "yes"),
        )
        assertEquals(listOf("Zalecane", "Główne", "Boczne"), shown(NAMES))
        assertEquals(listOf("Dostępne", "Dostępne"), shown(MARKINGS))
    }

    /**
     * Scenariusz: przegladanie-mapy.feature: Przy kilku wejściach dostępnych bez głównego zalecane jest
     *   pierwsze z danych.
     */
    @Test
    fun br25_data_order_without_main() {
        openCard(
            symbol = "C-13",
            entrance(name = "Boczne", wheelchair = "yes"),
            entrance(name = "Od dziedzińca", wheelchair = "yes"),
            entrance(name = "Główne", type = "main", wheelchair = "no"),
        )
        assertEquals(listOf("Zalecane", "Boczne", "Od dziedzińca", "Główne"), shown(NAMES))
        assertEquals(listOf("Dostępne", "Dostępne", "Niedostępne"), shown(MARKINGS))
    }

    /** Scenariusz: przegladanie-mapy.feature: Budynek bez wejścia dostępnego nie ma wejścia zalecanego. */
    @Test
    fun br25_none_without_accessible() {
        openCard(
            symbol = "B-8",
            entrance(name = "Główne", type = "main", wheelchair = "no"),
            entrance(name = "Boczne", wheelchair = "no"),
        )
        assertEquals(listOf("Główne", "Boczne"), shown(NAMES))
        assertEquals(listOf("Niedostępne", "Niedostępne"), shown(MARKINGS))
    }

    private fun openCard(symbol: String, vararg entrances: String) {
        val features =
            entrances.mapIndexed { index, tags ->
                """{"type":"Feature","properties":{"kind":"entrance","symbols":["$symbol"],""" +
                    """"osm_id":"node/$index",$tags},"geometry":{"type":"Point","coordinates":[0,0]}}"""
            }
        val repository =
            BuildingCatalogRepository(ioContext = EmptyCoroutineContext) {
                """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
                    """{"type":"Feature","properties":{"kind":"building","symbols":["$symbol"],""" +
                    """"display_name":"Budynek testowy","osm_id":"way/1"},""" +
                    """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}},""" +
                    features.joinToString(",") +
                    "]}"
            }
        val viewModel =
            BuildingCardViewModel(symbol = symbol, repository = repository, networkStatus = { flowOf(true) })
        compose.setContent {
            DostepnyKampusTheme {
                BuildingCardScreen(
                    symbol = symbol,
                    mode = CardMode.Expanded,
                    onCheckEntrances = {},
                    onShowOnMap = {},
                    onBack = {},
                    onClose = {},
                    viewModel = viewModel,
                )
            }
        }
    }

    private fun shown(wanted: Set<String>): List<String> =
        compose
            .onAllNodes(
                SemanticsMatcher("napis z listy wejść") { node ->
                    node.config
                        .getOrNull(SemanticsProperties.Text)
                        .orEmpty()
                        .any { it.text in wanted }
                },
                useUnmergedTree = true,
            )
            .fetchSemanticsNodes()
            .sortedBy { it.positionInRoot.y }
            .map { node -> node.config[SemanticsProperties.Text].first().text }

    private companion object {
        val NAMES = setOf("Zalecane", "Główne", "Boczne", "Od dziedzińca", "Północne", "Południowe", "Zachodnie")
        val MARKINGS = setOf("Dostępne", "Niedostępne", "Brak danych")

        fun entrance(name: String, type: String = "yes", wheelchair: String? = null): String =
            """"entrance":"$type","ref":"$name"""" + wheelchair?.let { ""","wheelchair":"$it"""" }.orEmpty()
    }
}
