/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
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
import org.robolectric.annotation.GraphicsMode

private const val SHOW = "Pokaż wejście na mapie"
private const val REPORT = "Zgłoś barierę przy wejściu"
private const val UNNAMED = "Wejście"
private const val NOBODY = "Nikt jeszcze nie sprawdził tych wejść."
private const val SIGN_TAG = "znak-oznaczenia"
private val TOUCH_TARGET = 48.dp

private val entranceRow =
    SemanticsMatcher("wiersz z czynnością „$SHOW”") { node ->
        node.config.getOrElseNullable(SemanticsActions.OnClick) { null }?.label == SHOW
    }

/**
 * Grupa „Wejścia” na V2: lista w jednym bloku z linią między wejściami, wiersz wejścia dotykalny w całości
 * i jeden wiersz dla kilku wejść bez oznaczenia i nazwy.
 *
 * Wymagania: US2.1, US3.3, US4.6, US17.1, NFR-17, NFR-18, NFR-21
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class EntrancesGroupTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val calls = mutableListOf<String>()

    /**
     * Wiersz: V2, znaki w jednej kolumnie, odstęp na linię między wierszami, wiersz ≥ 48 dp i szerszy niż nazwa.
     *
     * Wymagania: US2.1, NFR-17, NFR-21
     */
    @Test
    fun laysOutRowsInColumn() {
        group(NAMED)
        val signs = compose.onAllNodesWithTag(SIGN_TAG, useUnmergedTree = true).fetchSemanticsNodes()
        val lefts = signs.map { it.boundsInRoot.left }
        assertEquals("znak przy każdym wejściu", NAMED.size, lefts.size)
        assertTrue("znaki w jednej kolumnie: $lefts", lefts.distinct().size == 1)
        val rows = compose.onAllNodes(entranceRow).fetchSemanticsNodes().map { it.boundsInRoot }
        assertTrue("odstęp na linię: $rows", rows.zipWithNext().all { (up, down) -> down.top > up.bottom })
        val text = compose.onNodeWithText("Główne", useUnmergedTree = true).fetchSemanticsNode()
        val name = text.boundsInRoot.width
        val minimum = TOUCH_TARGET.value * DENSITY
        assertTrue("wiersz 48 dp i szerszy niż nazwa: $rows", rows.all { it.height >= minimum && it.width > name })
    }

    /**
     * Wiersz: V2, dotknięcie wiersza każdego wejścia pokazuje na mapie to wejście; w wierszu nie ma
     *   „Zgłoś barierę przy wejściu” ani przycisku „Pokaż wejście na mapie”.
     *
     * Wymagania: US3.3, US4.6, NFR-18
     */
    @Test
    fun showsEachEntranceOnTap() {
        group(NAMED)
        compose.onAllNodes(entranceRow).assertCountEquals(NAMED.size)
        NAMED.forEach { compose.onNode(entranceRow and hasText(it.name ?: UNNAMED)).performClick() }
        assertEquals("każdy wiersz pokazuje swoje wejście", NAMED.map { "show ${it.id}" }, calls)
        compose.onAllNodes(hasText(REPORT)).assertCountEquals(0)
        compose.onAllNodes(hasText(SHOW)).assertCountEquals(0)
    }

    /**
     * Wiersz: V2, co najmniej dwa wejścia bez oznaczenia i nazwy → jeden wiersz „N wejścia” albo
     *   „N wejść” (2, 4, 5), „Nikt jeszcze nie sprawdził tych wejść.” i jeden odnośnik zgłoszenia bez wejścia.
     * Scenariusz: przegladanie-mapy.feature: Kilka wejść bez oznaczenia i bez nazwy stoi jako jeden wiersz
     *
     * Wymagania: US2.1, US17.1, US4.6, NFR-21
     */
    @Test
    fun collapsesNamelessEntrances() {
        val state = mutableStateOf(ready(nameless(3)))
        compose.setContent { Group(state.value) }
        compose.onNodeWithText("3 wejścia").assertExists()
        compose.onNodeWithText("Brak danych").assertExists()
        compose.onNodeWithText(NOBODY).assertExists()
        compose.onAllNodes(hasText(UNNAMED)).assertCountEquals(0)
        compose.onAllNodes(entranceRow).assertCountEquals(0)
        compose.onAllNodes(hasText(REPORT) and hasClickAction()).assertCountEquals(1)
        compose.onNodeWithText(REPORT).performClick()
        assertEquals("jeden odnośnik bez wejścia", listOf("report"), calls)
        listOf(2 to "2 wejścia", 4 to "4 wejścia", 5 to "5 wejść").forEach { (count, text) ->
            state.value = ready(nameless(count))
            compose.waitForIdle()
            compose.onNodeWithText(text).assertExists()
        }
    }

    /**
     * Wiersz: V2, jedno wejście bez oznaczenia i nazwy albo choć jedno wejście z oznaczeniem lub nazwą → zwykła
     *   lista, każde wejście osobnym wierszem.
     *
     * Wymagania: US2.1, US17.1
     */
    @Test
    fun listsWhenAnyEntranceKnown() {
        val state = mutableStateOf(ready(nameless(1)))
        compose.setContent { Group(state.value) }
        listOf(
            nameless(1),
            nameless(2) + entrance(id = "node/9", name = null, marking = EntranceMarking.INACCESSIBLE),
            nameless(2) + entrance(id = "node/9", name = "Brama", marking = EntranceMarking.NO_DATA),
        ).forEach { entrances ->
            state.value = ready(entrances)
            compose.waitForIdle()
            compose.onAllNodes(entranceRow).assertCountEquals(entrances.size)
            compose.onNodeWithText(NOBODY).assertDoesNotExist()
        }
    }

    /** Zrzut listy wejść do porównania z makietą „m-v2-karta-budynku”, motyw jasny. */
    @Test
    fun matchesLightMockup() {
        group(NAMED)
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut listy wejść do porównania z makietą „m-v2-karta-budynku-jasny-200”. */
    @Test
    @Config(fontScale = 2f)
    fun matchesLargeFontMockup() {
        group(NAMED)
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut zwiniętych wejść bez danych, motyw jasny. */
    @Test
    fun matchesNamelessMockup() {
        group(nameless(3))
        compose.onRoot().captureRoboImage()
    }

    private fun group(entrances: List<Entrance>) {
        val state = ready(entrances)
        compose.setContent { Group(state) }
    }

    @Composable
    private fun Group(card: BuildingCardUiState.Ready) {
        DostepnyKampusTheme {
            Column(Modifier.background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
                EntrancesGroup(
                    card = card,
                    withAssessment = true,
                    onShowEntrance = { calls += "show $it" },
                    onReport = { calls += "report" },
                )
            }
        }
    }

    private companion object {
        const val DENSITY = 2f

        val NAMED =
            listOf(
                entrance(id = "node/1", name = "Główne", marking = EntranceMarking.ACCESSIBLE),
                entrance(id = "node/2", name = "Schody", marking = EntranceMarking.INACCESSIBLE),
                entrance(id = "node/3", name = null, marking = EntranceMarking.NO_DATA),
            )

        fun nameless(count: Int): List<Entrance> =
            (1..count).map { entrance(id = "node/$it", name = null, marking = EntranceMarking.NO_DATA) }

        fun ready(entrances: List<Entrance>): BuildingCardUiState.Ready =
            BuildingCardUiState.Ready(symbol = "C-7", name = null, entrances = orderedEntrances(entrances))

        fun entrance(id: String, name: String?, marking: EntranceMarking): Entrance =
            Entrance(
                id = id,
                buildingSymbol = "C-7",
                name = name,
                isMain = false,
                type = "yes",
                location = GeoPoint(longitude = 0.5, latitude = 0.5),
                marking = marking,
            )
    }
}
