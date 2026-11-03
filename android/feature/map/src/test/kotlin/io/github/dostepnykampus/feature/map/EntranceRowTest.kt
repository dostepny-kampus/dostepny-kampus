/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.coroutines.EmptyCoroutineContext

private val ROWS = listOf("Wejście dostępne", "Brak wejścia dostępnego", "Brak danych o wejściach")

/** Wiersz o wejściach na V2 Karta budynku (US2.8, US3.1). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class EntranceRowTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    // Wiersz: V2, wiersz o wejściach pod nagłówkiem, nad grupą „Wejścia”, słowami: „Wejście dostępne”, „Brak
    //   wejścia dostępnego”, gdy każde „Niedostępne”, „Brak danych o wejściach”, gdy żadne nie jest „Dostępne”,
    //   a choć jedno ma „Brak danych”, US2.8, US2.1, US3.1, NFR-21.
    @Test
    fun us2_8_row_says_entrance_state() {
        val shown = mutableStateOf("A-1")
        compose.setContent {
            val symbol = shown.value
            val viewModel =
                remember(symbol) {
                    BuildingCardViewModel(symbol = symbol, repository = repository, networkStatus = { flowOf(true) })
                }
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
        listOf("A-1" to ROWS[0], "B-4" to ROWS[1], "C-13" to ROWS[2]).forEach { (symbol, row) ->
            shown.value = symbol
            compose.waitUntil { compose.onAllNodes(hasText("Budynek $symbol")).fetchSemanticsNodes().isNotEmpty() }
            val tops =
                listOf("Budynek $symbol", row, "Wejścia").map {
                    compose.onNodeWithText(it).getUnclippedBoundsInRoot().top
                }
            assertTrue("$symbol: nagłówek, „$row”, „Wejścia” od góry, a są na $tops", tops == tops.sorted())
            (ROWS - row).forEach { compose.onNodeWithText(it).assertDoesNotExist() }
        }
    }

    private companion object {
        val repository =
            BuildingCatalogRepository(EmptyCoroutineContext) {
                listOf(
                    building(symbol = "A-1"),
                    entrance(symbol = "A-1", tags = """"wheelchair":"no""""),
                    entrance(symbol = "A-1", tags = """"wheelchair":"yes""""),
                    building(symbol = "B-4"),
                    entrance(symbol = "B-4", tags = """"wheelchair":"no""""),
                    building(symbol = "C-13"),
                    entrance(symbol = "C-13", tags = """"wheelchair":"no""""),
                    entrance(symbol = "C-13", tags = """"ref":"Brama""""),
                ).joinToString(
                    separator = ",",
                    prefix = """{"type":"FeatureCollection","attribution":"©","features":[""",
                    postfix = "]}",
                )
            }

        fun building(symbol: String): String =
            """{"type":"Feature","properties":{"kind":"building","symbols":["$symbol"],"osm_id":"way/$symbol"},""" +
                """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}"""

        fun entrance(symbol: String, tags: String): String =
            """{"type":"Feature","properties":{"kind":"entrance","symbols":["$symbol"],""" +
                """"osm_id":"node/$symbol-${tags.hashCode()}","entrance":"yes",$tags},""" +
                """"geometry":{"type":"Point","coordinates":[0,0]}}"""
    }
}
