/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.buildings.ServerEntrances
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.AssessmentSource
import io.github.dostepnykampus.core.model.AssessmentSource.IMPORT
import io.github.dostepnykampus.core.model.AssessmentSource.WALKTHROUGH
import io.github.dostepnykampus.core.model.Entrance
import io.github.dostepnykampus.core.model.EntranceAssessment
import io.github.dostepnykampus.core.model.EntranceMarking
import io.github.dostepnykampus.core.model.GeoPoint
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import kotlin.coroutines.EmptyCoroutineContext

private val DATE_TEXT = Regex("""\d{1,2}\.\d{1,2}\.\d{4}|\d{4}-\d{2}-\d{2}""")

private val hasDate =
    SemanticsMatcher("ma datę") { node ->
        node.config.getOrNull(SemanticsProperties.Text)?.any { DATE_TEXT.containsMatchIn(it.text) } == true
    }

/** Data i źródło oznaczenia wejść na V2 Karta budynku i V3 Którędy wejść (US17.3, BR-24, PW-3). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class EntranceAssessmentTextTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Wiersz: V2, wszystkie wejścia z importu z tym samym dniem → jeden wiersz „Z map i opisów, stan z 1.10.2026”
     *   nad grupą „Wejścia”, US17.3, BR-24, import.
     */
    @Test
    fun us17_3_v2_common_row_import() {
        showCard(listOf(entrance(MAIN, IMPORT, IMPORT_DAY), entrance(SIDE, IMPORT, IMPORT_DAY)))
        compose.onNodeWithText(IMPORT_TEXT).assertExists()
        assertEquals(1, datedCount())
        assertTrue(top(IMPORT_TEXT) < top(GROUP_V2))
    }

    /**
     * Wiersz: V2, wszystkie wejścia z obchodu z tym samym dniem → jeden wiersz „Sprawdzone na miejscu 5.3.2026”
     *   (bez zer wiodących, bez „Z map i opisów”), US17.3, BR-24, obchód.
     */
    @Test
    fun us17_3_v2_row_walkthrough() {
        showCard(listOf(entrance(MAIN, WALKTHROUGH, WALK_DAY), entrance(SIDE, WALKTHROUGH, WALK_DAY)))
        compose.onNodeWithText(WALK_TEXT).assertExists()
        assertEquals(1, datedCount())
        compose.onNodeWithText("Z map i opisów", substring = true).assertDoesNotExist()
        assertTrue(top(WALK_TEXT) < top(GROUP_V2))
    }

    /** Wiersz: V2, wejścia z różnym źródłem → napis przy każdym wejściu, nad grupą go nie ma, US17.3, BR-24. */
    @Test
    fun us17_3_v2_per_entrance() {
        showCard(listOf(entrance(MAIN, IMPORT, IMPORT_DAY), entrance(SIDE, WALKTHROUGH, WALK_DAY)))
        assertPerEntrance(first = IMPORT_TEXT, second = WALK_TEXT, group = GROUP_V2)
    }

    /**
     * Wiersz: V2, wejścia z tym samym źródłem i różnym dniem → napis przy każdym wejściu, nad grupą go nie ma,
     *   US17.3, BR-24, import.
     */
    @Test
    fun us17_3_v2_per_day() {
        showCard(listOf(entrance(MAIN, IMPORT, IMPORT_DAY), entrance(SIDE, IMPORT, NEXT_DAY)))
        assertPerEntrance(first = IMPORT_TEXT, second = NEXT_TEXT, group = GROUP_V2)
    }

    /**
     * Wiersz: V3, wszystkie wejścia z importu z tym samym dniem → jeden wiersz „Z map i opisów, stan z 1.10.2026”
     *   nad wejściami, US17.3, BR-24, import.
     */
    @Test
    fun us17_3_v3_common_row() {
        showVisit(listOf(entrance(MAIN, IMPORT, IMPORT_DAY), entrance(SIDE, IMPORT, IMPORT_DAY)))
        compose.onNodeWithText(IMPORT_TEXT).assertExists()
        assertEquals(1, datedCount())
        assertTrue(top(IMPORT_TEXT) < top(GROUP_V3))
    }

    /** Wiersz: V3, wejścia z różnym źródłem → napis przy każdym wejściu, nad wejściami go nie ma, US17.3, BR-24. */
    @Test
    fun us17_3_v3_per_entrance() {
        showVisit(listOf(entrance(MAIN, IMPORT, IMPORT_DAY), entrance(SIDE, WALKTHROUGH, WALK_DAY)))
        assertPerEntrance(first = IMPORT_TEXT, second = WALK_TEXT, group = GROUP_V3)
    }

    /** Wiersz: V2, wejścia z pliku bez dnia i źródła → brak wiersza i brak jakiejkolwiek daty, US17.3, BR-24. */
    @Test
    fun us17_3_v2_file_has_no_date() {
        showCard(emptyList())
        assertEquals(0, datedCount())
        listOf("stan z", "Sprawdzone").forEach { compose.onNodeWithText(it, substring = true).assertDoesNotExist() }
    }

    /** Wiersz: V3, wejścia z pliku → brak daty, zdanie „brak danych o wejściach” bez „z datą”, US17.3, PW-10. */
    @Test
    fun us17_3_v3_file_has_no_date() {
        showVisit(emptyList())
        compose.onNodeWithText("zobaczyć każde wejście.", substring = true).assertExists()
        assertEquals(0, datedCount())
        compose.onNodeWithText("z datą", substring = true).assertDoesNotExist()
    }

    private fun assertPerEntrance(first: String, second: String, group: String) {
        compose.onNodeWithText(first).assertExists()
        compose.onNodeWithText(second).assertExists()
        assertEquals(2, datedCount())
        assertTrue(top(group) < top(first) && top(first) < top(second))
    }

    private fun showCard(server: List<Entrance>) =
        show(server) { viewModel ->
            BuildingCardScreen(
                symbol = SYMBOL,
                mode = CardMode.Expanded,
                onCheckEntrances = {},
                onShowOnMap = {},
                onBack = {},
                onClose = {},
                viewModel = viewModel,
            )
        }

    private fun showVisit(server: List<Entrance>) =
        show(server, if (server.isEmpty()) "brak danych o 1 z 1" else null) { viewModel ->
            EntrancesScreen(symbol = SYMBOL, onShowEntrance = {}, onBack = {}, viewModel = viewModel)
        }

    private fun show(
        server: List<Entrance>,
        awaited: String? = null,
        screen: @Composable (BuildingCardViewModel) -> Unit,
    ) {
        val viewModel =
            BuildingCardViewModel(
                symbol = SYMBOL,
                repository = catalog,
                networkStatus = { flowOf(true) },
                serverEntrances = ServerEntrances { server },
            )
        compose.setContent { DostepnyKampusTheme { screen(viewModel) } }
        val expected = awaited ?: server.firstOrNull()?.name ?: FILE_ENTRANCE
        compose.waitUntil { compose.onAllNodes(hasText(expected, substring = true)).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun datedCount(): Int = compose.onAllNodes(hasDate).fetchSemanticsNodes().size

    private fun top(text: String) = compose.onNodeWithText(text).getUnclippedBoundsInRoot().top

    private fun entrance(name: String, source: AssessmentSource, day: LocalDate) =
        Entrance(
            id = name,
            buildingSymbol = SYMBOL,
            name = name,
            isMain = name == MAIN,
            type = null,
            location = GeoPoint(longitude = 0.0, latitude = 0.0),
            marking = EntranceMarking.ACCESSIBLE,
            assessment = EntranceAssessment(day, source),
        )

    private companion object {
        const val SYMBOL = "A-1"
        const val MAIN = "Główne"
        const val SIDE = "Boczne"
        const val FILE_ENTRANCE = "Brama"
        const val GROUP_V2 = "Wejścia"
        const val GROUP_V3 = "Zalecane wejście"
        const val IMPORT_TEXT = "Z map i opisów, stan z 1.10.2026"
        const val NEXT_TEXT = "Z map i opisów, stan z 2.10.2026"
        const val WALK_TEXT = "Sprawdzone na miejscu 5.3.2026"

        val IMPORT_DAY: LocalDate = LocalDate.of(2026, 10, 1)
        val NEXT_DAY: LocalDate = LocalDate.of(2026, 10, 2)
        val WALK_DAY: LocalDate = LocalDate.of(2026, 3, 5)

        val catalog =
            BuildingCatalogRepository(EmptyCoroutineContext) {
                """{"type":"FeatureCollection","attribution":"©","features":[""" +
                    """{"type":"Feature","properties":{"kind":"building","symbols":["A-1"],"osm_id":"way/1"},""" +
                    """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}},""" +
                    """{"type":"Feature","properties":{"kind":"entrance","symbols":["A-1"],"osm_id":"node/1",""" +
                    """"entrance":"yes","ref":"Brama"},"geometry":{"type":"Point","coordinates":[0,0]}}]}"""
            }
    }
}
