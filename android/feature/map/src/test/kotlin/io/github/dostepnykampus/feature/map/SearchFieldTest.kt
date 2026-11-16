/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.geo.building.searchBuildings
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.feature.map.area.AreaOrder
import io.github.dostepnykampus.feature.map.building.BuildingSearch
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Pole symbolu V1 po wyborze wyniku wyszukiwania, na budynku testowym „Budynek testowy” (nie dane PWr). */
@RunWith(RobolectricTestRunner::class)
class SearchFieldTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Wiersz: V1 z małą kartą V2, tekst wyszukiwania bez zmian. Wybór wyniku zostawia „c13” w polu i chowa wyniki,
     * a dotknięcie pola pokazuje je z powrotem.
     *
     * Wymagania: US1.1, UC1
     */
    @Test
    fun keepsQueryAfterChoice() {
        val opened = mutableListOf<String>()
        compose.setContent { Search(onBuildingClick = { opened += it }) }
        val field = compose.onNode(hasSetTextAction())
        field.performTextInput("c13")
        compose.onNodeWithContentDescription(RESULT).performClick()

        assertEquals("otwarty budynek", listOf("C-13"), opened)
        field.assertTextEquals("c13", includeEditableText = true)
        compose.onNodeWithContentDescription(RESULT).assertDoesNotExist()
        field.performClick()
        compose.onNodeWithContentDescription(RESULT).assertExists()
    }

    @Composable
    private fun Search(onBuildingClick: (String) -> Unit) {
        val catalog = BuildingCatalog(listOf(unitSquareBuilding("C-13")), entrances = emptyList(), attribution = "")
        var search by remember { mutableStateOf(BuildingSearch()) }
        DostepnyKampusTheme {
            CampusSearch(
                catalog = catalog,
                search = search,
                onSearch = { query -> search = BuildingSearch(query, searchBuildings(query, catalog.buildings)) },
                isOnline = true,
                syncedAt = null,
                isList = false,
                areas = AreaOrder(emptyList()),
                onListChange = {},
                onBuildingClick = onBuildingClick,
            )
        }
    }

    private companion object {
        const val RESULT = "C-13, Budynek testowy"
    }
}
