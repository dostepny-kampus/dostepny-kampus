/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.percentOffset
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.feature.map.camera.CampusMapView
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.coroutines.EmptyCoroutineContext

/** Pole „Szukaj” V1 nad widokiem zastępczym mapy, na budynku testowym C-13 (nie dane PWr). */
@RunWith(RobolectricTestRunner::class)
class MapSearchCloseTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val map =
        CampusMapView { _: CampusMapData, _, _, _, _, _, _, modifier: Modifier ->
            Box(modifier.testTag(MAP))
        }
    private val repository =
        BuildingCatalogRepository(ioContext = EmptyCoroutineContext) {
            """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["C-13"]},"geometry":""" +
                """{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}]}"""
        }

    /**
     * Wiersz: V1 z otwartą klawiaturą. Dotknięcie mapy pod podpowiedziami zdejmuje fokus z pola i chowa
     * podpowiedzi, a tekst w polu zostaje.
     *
     * Wymagania: US3.1, NFR-17
     */
    @Test
    fun mapTouchClosesSearch() {
        compose.showMap(viewModel = testMapViewModel(repository), map = map)
        val field = compose.onNode(hasSetTextAction())
        field.performTextInput("C")
        field.assertIsFocused()
        compose.onNode(hasContentDescription("C-13") and hasClickAction()).assertExists()
        compose.onNodeWithTag(MAP).performTouchInput {
            down(percentOffset(x = 0.5f, y = 0.9f))
            up()
        }

        field.assertIsNotFocused()
        field.assertTextEquals("C", includeEditableText = true)
        compose.onNode(hasContentDescription("C-13") and hasClickAction()).assertDoesNotExist()
    }

    private companion object {
        const val MAP = "mapa"
    }
}
