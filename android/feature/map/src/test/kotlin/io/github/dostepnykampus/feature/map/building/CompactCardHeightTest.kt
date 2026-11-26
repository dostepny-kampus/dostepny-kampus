/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.coroutines.EmptyCoroutineContext

private const val CARD = "karta"
private const val NAME = "Gmach Główny"

/**
 * Mała karta V2 ma wysokość gotowej treści od pierwszej klatki: ruch mapy do budynku liczony jest raz, dla pierwszej
 * wysokości karty.
 *
 * Wymagania: US2.8, NFR-56
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class CompactCardHeightTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** Wiersz: V2, mała karta przed wczytaniem opisu ma wysokość gotowej karty, a przyciski stoją na miejscu. */
    @Test
    fun keepsHeightWhileLoading() {
        val card = mutableStateOf<BuildingCardUiState>(BuildingCardUiState.Loading)
        compose.setContent {
            DostepnyKampusTheme {
                Box(Modifier.testTag(CARD)) {
                    BuildingCardContent(
                        symbol = "A-1",
                        mode = CardMode.Compact,
                        card = card.value,
                        isOnline = true,
                        onShowOnMap = {},
                        onBack = {},
                        onClose = {},
                        onReport = {},
                        onRetry = {},
                    )
                }
            }
        }
        val loading = cardHeight()
        val close = compose.onNodeWithText("Zamknij").getUnclippedBoundsInRoot().top
        compose.onNodeWithText(NAME).assertDoesNotExist()
        card.value = BuildingCardUiState.Ready(symbol = "A-1", name = NAME, entrances = emptyList())
        compose.waitForIdle()
        assertEquals("wysokość karty przed wczytaniem i po", cardHeight(), loading, 1f)
        val ready = compose.onNodeWithText("Zamknij").getUnclippedBoundsInRoot().top
        assertEquals("„Zamknij” na tym samym miejscu", ready.value, close.value, 1f)
    }

    /** Wiersz: V2, mała karta budynku z katalogu już wczytanego przez mapę jest gotowa od pierwszej klatki. */
    @Test
    fun startsReadyFromLoadedCatalog() {
        runBlocking { repository.catalog() }
        val viewModel = BuildingCardViewModel(symbol = "A-1", repository = repository, networkStatus = { flowOf(true) })
        val first = viewModel.uiState.value
        assertTrue("karta od razu gotowa, a jest $first", first is BuildingCardUiState.Ready)
    }

    private fun cardHeight(): Float {
        val bounds = compose.onNodeWithTag(CARD).getUnclippedBoundsInRoot()
        return (bounds.bottom - bounds.top).value
    }

    private companion object {
        val repository =
            BuildingCatalogRepository(EmptyCoroutineContext) {
                """{"type":"FeatureCollection","attribution":"©","features":[""" +
                    """{"type":"Feature","properties":{"kind":"building","symbols":["A-1"],""" +
                    """"display_name":"$NAME","osm_id":"way/1"},""" +
                    """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}]}"""
            }
    }
}
