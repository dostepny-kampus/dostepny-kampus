/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val NO_DATA = "Brak danych o wejściach"
private const val SHOW_ENTRANCE = "Pokaż wejście na mapie"
private const val SYMBOL_WEIGHT = 800
private const val SYMBOL_SP = 36
private const val NAME_SP = 22

/** Nagłówek karty V2: symbol, nazwa i odpowiedź o wejściach. Wymagania: US2, US3.1, NFR-16, NFR-18 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class BuildingCardHeaderTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** Wiersz wymagań V2 „Nagłówek”: symbol, pod nim nazwa, a TalkBack czyta „Budynek A-1, Gmach Główny”. */
    @Test
    fun showsSymbolAndName() {
        show(symbol = "A-1", name = "Gmach Główny")
        compose.onNodeWithText("A-1", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Gmach Główny", useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasContentDescription("Budynek A-1, Gmach Główny")).assertIsFocused()
    }

    /** Wiersz wymagań V2 „Nagłówek”: bez nazwy sam symbol, a TalkBack czyta „Budynek A-4”. */
    @Test
    fun showsSymbolOnly() {
        show(symbol = "A-4", name = null)
        compose.onNodeWithText("A-4", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Budynek A-4", useUnmergedTree = true).assertDoesNotExist()
        compose.onNode(hasContentDescription("Budynek A-4")).assertIsFocused()
    }

    /**
     * Wiersz wymagań V2 „Nagłówek”: bez danych o wejściach pod nazwą cichy napis „Brak danych o wejściach”, bez
     *   zdania i bez „Pokaż wejście na mapie”.
     *
     * Wymagania: US3.1, BR-25
     */
    @Test
    fun showsQuietLineWithoutEntrances() {
        show(symbol = "A-1", name = "Gmach Główny")
        val name = compose.onNodeWithText("Gmach Główny", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val line = compose.onNodeWithText(NO_DATA).getUnclippedBoundsInRoot()
        assertTrue("„$NO_DATA” pod nazwą: $line, nazwa $name", line.top >= name.bottom)
        compose.onAllNodes(hasText(NO_DATA)).assertCountEquals(1)
        compose.onAllNodes(hasText(SHOW_ENTRANCE)).assertCountEquals(0)
        compose.onAllNodes(hasText("Zalecane:", substring = true)).assertCountEquals(0)
    }

    /**
     * Wiersz wymagań V2 „Odpowiedź”: pod wierszem o wejściach zdanie „Zalecane: „nazwa”.”, czytane z wierszem jako
     *   jeden tekst, a pod nim wypełnione „Pokaż wejście na mapie”; dotknięcie podaje id wejścia zalecanego.
     *
     * Wymagania: US3.1, US3.2, BR-25, NFR-18
     */
    @Test
    fun answersWithRecommended() {
        val shown = mutableListOf<String>()
        show(
            entrances =
                listOf(door("Boczne", EntranceMarking.INACCESSIBLE), door("Główne", EntranceMarking.ACCESSIBLE)),
            onShowEntrance = { shown += it },
        )
        val answer = compose.onNode(hasText("Wejście dostępne") and hasText("Zalecane: „Główne”."))
        val button = compose.onNode(hasContentDescription("$SHOW_ENTRANCE: »Główne«"))
        val line = answer.getUnclippedBoundsInRoot()
        val below = button.getUnclippedBoundsInRoot()
        assertTrue("zdanie nad przyciskiem: $line, $below", line.bottom <= below.top)
        button.performClick()
        assertEquals("id wejścia zalecanego", listOf("Główne"), shown)
    }

    /**
     * Wiersz wymagań V2 „Odpowiedź”: wejście zalecane bez nazwy jest w zdaniu opisane słowem „Wejście” bez
     *   cudzysłowu. Wymagania: US3.1, BR-27
     */
    @Test
    fun answersWithUnnamed() {
        show(entrances = listOf(door("Boczne", EntranceMarking.ACCESSIBLE, name = null)))
        compose.onNodeWithText("Zalecane: Wejście.").assertExists("zdanie bez nazwy")
    }

    /**
     * Wiersz wymagań V2 „Odpowiedź”: bez wejścia dostępnego zdanie mówi, ile wejść sprawdzono, i nie ma
     *   wypełnionego przycisku.
     *
     * Wymagania: US3.1, BR-25
     */
    @Test
    fun answersWithoutAccessible() {
        val doors = listOf("Schody", "Brama", "Rampa").map { door(it, EntranceMarking.INACCESSIBLE) }
        show(entrances = doors)
        compose.onNodeWithText("Żadne z 3 wejść nie jest dostępne.").assertExists("zdanie o trzech wejściach")
        compose.onAllNodes(hasText(SHOW_ENTRANCE)).assertCountEquals(0)
    }

    /** Wiersz wymagań V2 „Odpowiedź”: jedyne wejście niedostępne ma własne zdanie. Wymagania: US3.1, BR-25 */
    @Test
    fun answersForOnlyEntrance() {
        show(entrances = listOf(door("Schody", EntranceMarking.INACCESSIBLE)))
        compose.onNodeWithText("Jedyne wejście nie jest dostępne.").assertExists("zdanie o jedynym wejściu")
    }

    /** Wiersz wymagań V2 „Nagłówek”: symbol 36 sp z wagą 800, nazwa pod nim 22 sp, jak nagłówek grupy. */
    @Test
    fun usesHeaderTextSizes() {
        show(symbol = "A-1", name = "Gmach Główny")
        val symbol = styleOf("A-1")
        assertEquals("rozmiar symbolu", SYMBOL_SP.sp, symbol.fontSize)
        assertEquals("waga symbolu", FontWeight(SYMBOL_WEIGHT), symbol.fontWeight)
        assertEquals("rozmiar nazwy", NAME_SP.sp, styleOf("Gmach Główny").fontSize)
    }

    private fun styleOf(text: String): TextStyle {
        val layouts = mutableListOf<TextLayoutResult>()
        compose
            .onNodeWithText(text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        return layouts.single().layoutInput.style
    }

    private fun door(id: String, marking: EntranceMarking, name: String? = id) =
        Entrance(
            id = id,
            buildingSymbol = "A-1",
            name = name,
            isMain = false,
            type = "yes",
            location = GeoPoint(longitude = 0.0, latitude = 0.0),
            marking = marking,
        )

    private fun show(
        symbol: String = "A-1",
        name: String? = "Gmach Główny",
        entrances: List<Entrance> = emptyList(),
        onShowEntrance: (String) -> Unit = {},
    ) {
        val ordered = orderedEntrances(entrances)
        compose.setContent {
            DostepnyKampusTheme {
                BuildingCardContent(
                    symbol = symbol,
                    mode = CardMode.Compact,
                    card = BuildingCardUiState.Ready(symbol = symbol, name = name, entrances = ordered),
                    isOnline = true,
                    onShowOnMap = {},
                    onBack = {},
                    onClose = {},
                    onReport = {},
                    onRetry = {},
                    onShowEntrance = onShowEntrance,
                )
            }
        }
    }
}
