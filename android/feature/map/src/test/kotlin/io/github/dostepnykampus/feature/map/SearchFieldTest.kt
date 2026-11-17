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
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
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

    /**
     * Pierwsza litera pokazuje budynki w kolejności katalogu, bez komunikatu o braku wyniku.
     *
     * Scenariusz: przegladanie-mapy.feature: Podpowiedzi budynków od pierwszego znaku symbolu
     *
     * Wymagania: US1.1
     */
    @Test
    fun suggestsFromFirstLetter() {
        compose.setContent { Search(onBuildingClick = {}, catalog = catalogOf("A-1", "A-2", "A-10", "C-13")) }
        compose.onNode(hasSetTextAction()).performTextInput("A")
        val shown =
            compose.onAllNodes(hasContentDescription("Budynek testowy", substring = true)).fetchSemanticsNodes().map {
                it.config[SemanticsProperties.ContentDescription].single()
            }
        assertEquals("podpowiedzi", listOf("A-1", "A-2", "A-10").map { "$it, Budynek testowy" }, shown)
        compose.onNodeWithText("Nie znaleźliśmy budynku", substring = true).assertDoesNotExist()
    }

    /**
     * Wynik dopasowany po nazwie otwiera budynek tak samo jak wynik po symbolu.
     *
     * Scenariusz: przegladanie-mapy.feature: Wyszukanie budynku po nazwie
     *
     * Wymagania: US1.1, UC1
     */
    @Test
    fun opensBuildingFoundByName() {
        val opened = mutableListOf<String>()
        val main = unitSquareBuilding("A-1").copy(name = "Budynek główny")
        val buildings = listOf(main, unitSquareBuilding("C-13"))
        val catalog = BuildingCatalog(buildings, entrances = emptyList(), attribution = "")
        compose.setContent { Search(onBuildingClick = { opened += it }, catalog = catalog) }
        compose.onNode(hasSetTextAction()).performTextInput("glow")
        compose.onNodeWithContentDescription(RESULT).assertDoesNotExist()
        compose.onNodeWithContentDescription("A-1, Budynek główny").performClick()

        assertEquals("otwarty budynek", listOf("A-1"), opened)
    }

    @Composable
    private fun Search(onBuildingClick: (String) -> Unit, catalog: BuildingCatalog = catalogOf("C-13")) {
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

    private fun catalogOf(vararg symbols: String): BuildingCatalog =
        BuildingCatalog(symbols.map(::unitSquareBuilding), entrances = emptyList(), attribution = "")

    private companion object {
        const val RESULT = "C-13, Budynek testowy"
    }
}
