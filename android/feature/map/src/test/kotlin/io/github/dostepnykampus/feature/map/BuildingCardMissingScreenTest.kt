/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.EmptyCoroutineContext

private const val MISSING_TEXT = "Tego budynku nie ma jeszcze w telefonie"
private const val RETRY_TEXT = "Spróbuj ponownie"
private const val GMACH =
    """{"type":"Feature","properties":{"kind":"building","symbols":["A-1"],"name":"Gmach Główny","osm_id":"way/1"},""" +
        """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}"""
private const val NEW_BUILDING =
    """{"type":"Feature","properties":{"kind":"building","symbols":["N-1"],"name":"Nowy budynek","osm_id":"way/2"},""" +
        """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}"""

/** V2 Karta budynku, gdy budynku nie ma w katalogu zapisanym w telefonie (US2.7, NFR-10). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class BuildingCardMissingScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun showCard(symbol: String, features: () -> List<String> = { listOf(GMACH) }) {
        val repository =
            BuildingCatalogRepository(ioContext = EmptyCoroutineContext) {
                """{"type":"FeatureCollection","attribution":"© OpenStreetMap",""" +
                    """"features":[${features().joinToString(",")}]}"""
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

    // Wiersz: V2, symbolu nie ma w katalogu → napis „Tego budynku nie ma jeszcze w telefonie” i „Spróbuj ponownie”,
    //   NFR-10, US2.7.
    @Test
    fun nfr10_unknown_shows_missing() {
        showCard("Z-9")
        compose.onNodeWithText(MISSING_TEXT, substring = true).assertIsDisplayed()
        compose.onNodeWithText(RETRY_TEXT).assertIsDisplayed()
    }

    // Wiersz: V2, symbol jest w katalogu → brak napisu „Tego budynku nie ma jeszcze w telefonie”, NFR-10, US2.7.
    @Test
    fun nfr10_known_hides_missing() {
        showCard("A-1")
        compose.onNodeWithText("Gmach Główny").assertIsDisplayed()
        compose.onNodeWithText(MISSING_TEXT, substring = true).assertDoesNotExist()
        compose.onNodeWithText(RETRY_TEXT).assertDoesNotExist()
    }

    // Wiersz: V2, budynku nie ma w telefonie, „Spróbuj ponownie” → katalog czytany od nowa, a gdy budynek już w nim
    //   jest, karta budynku zamiast napisu, NFR-10.
    @Test
    fun nfr10_retry_click_reads_again() {
        val reads = AtomicInteger()
        showCard("N-1") { if (reads.incrementAndGet() == 1) listOf(GMACH) else listOf(GMACH, NEW_BUILDING) }
        compose.onNodeWithText(MISSING_TEXT, substring = true).assertIsDisplayed()
        compose.onNodeWithText(RETRY_TEXT).performClick()
        compose.onNodeWithText("Nowy budynek").assertIsDisplayed()
        compose.onNodeWithText(MISSING_TEXT, substring = true).assertDoesNotExist()
    }
}
