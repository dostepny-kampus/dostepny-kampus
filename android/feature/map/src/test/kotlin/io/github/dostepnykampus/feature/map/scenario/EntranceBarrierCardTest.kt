/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.scenario

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.data.barrier.AcceptedBarriers
import io.github.dostepnykampus.core.data.building.ServerEntrances
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.feature.map.building.BuildingCardScreen
import io.github.dostepnykampus.feature.map.building.BuildingCardViewModel
import io.github.dostepnykampus.feature.map.building.CardMode
import io.github.dostepnykampus.feature.map.building.entranceBarrier
import io.github.dostepnykampus.feature.map.building.sampleEntrance
import io.github.dostepnykampus.feature.map.unitSquareCatalog
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

private const val RECOMMENDED = "Zalecane"
private const val ACCESSIBLE = "Dostępne"
private const val INACCESSIBLE = "Niedostępne"
private const val MAIN = "Wejście główne"
private const val SIDE = "Wejście boczne"
private const val YARD = "Wejście od dziedzińca"
private const val SHOW_ENTRANCE = "Pokaż wejście na mapie"
private const val THRESHOLD_LINE = "Utrudnienie: próg"
private const val HEAVY_DOOR_LINE = "Tylko z pomocą: ciężkie drzwi"
private const val BLOCKED_LINE = "Blokada: schody przy wejściu"

/**
 * Wejście zalecane i kolejność wejść na karcie budynku C-13 przy barierach przyjętych przy wejściach.
 *
 * Wymagania: US3.1, BR-25
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class EntranceBarrierCardTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Scenariusz (przykład: Utrudnienie | żadnej | wejście boczne): przegladanie-mapy.feature:
     *   Wejście bez barier jest zalecane przed wejściem z barierą
     */
    @Test
    fun prefersClearOverHindrance() {
        val barriers = listOf(entranceBarrier("main", Severity.HINDRANCE, BarrierType.THRESHOLD))
        openCard(threeEntrances(), barriers)

        compose.onNodeWithText("Wejście dostępne").assertExists("Wiersz o wejściach mówi „Wejście dostępne”")
        compose
            .onNodeWithText("Zalecane: „Wejście boczne”. Drzwi 100 cm.")
            .assertExists("Zdanie pod wierszem wskazuje wejście boczne bez barier")
        assertRow(name = SIDE, marking = ACCESSIBLE, recommended = true)
        assertRow(name = MAIN, marking = ACCESSIBLE, recommended = false, barrier = THRESHOLD_LINE)
        assertRow(name = YARD, marking = INACCESSIBLE, recommended = false)
        assertEquals(
            "Kolejność pozostałych wejść na liście „Wejścia” się nie zmienia",
            listOf(SIDE, MAIN, YARD),
            rowOrder(MAIN, SIDE, YARD),
        )
    }

    /**
     * Scenariusz (przykład: Tylko z pomocą): przegladanie-mapy.feature:
     *   Wejście bez barier jest zalecane przed wejściem z barierą
     */
    @Test
    fun prefersLighterBarrier() {
        val barriers =
            listOf(
                entranceBarrier("main", Severity.ONLY_WITH_HELP, BarrierType.HEAVY_DOOR),
                entranceBarrier("side", Severity.HINDRANCE, BarrierType.THRESHOLD),
            )
        openCard(threeEntrances(), barriers)

        compose.onNodeWithText("Wejście dostępne").assertExists("Wiersz o wejściach mówi „Wejście dostępne”")
        compose
            .onNodeWithText("Zalecane: „Wejście boczne”. Drzwi 100 cm. Przy wejściu jest bariera: utrudnienie.")
            .assertExists("Zdanie pod wierszem wskazuje wejście boczne z najlżejszą barierą")
        assertRow(name = SIDE, marking = ACCESSIBLE, recommended = true, barrier = THRESHOLD_LINE)
        assertRow(name = MAIN, marking = ACCESSIBLE, recommended = false, barrier = HEAVY_DOOR_LINE)
        assertRow(name = YARD, marking = INACCESSIBLE, recommended = false)
        assertEquals(
            "Kolejność pozostałych wejść na liście „Wejścia” się nie zmienia",
            listOf(SIDE, MAIN, YARD),
            rowOrder(MAIN, SIDE, YARD),
        )
    }

    /**
     * Scenariusz (przykład: Utrudnienie | przyjęta bariera „Utrudnienie” | wejście główne): przegladanie-mapy.feature:
     *   Wejście bez barier jest zalecane przed wejściem z barierą
     */
    @Test
    fun keepsMainAtEqualBarriers() {
        val barriers =
            listOf(
                entranceBarrier("main", Severity.HINDRANCE, BarrierType.THRESHOLD),
                entranceBarrier("side", Severity.HINDRANCE, BarrierType.HEAVY_DOOR),
            )
        openCard(threeEntrances(), barriers)

        compose.onNodeWithText("Wejście dostępne").assertExists("Wiersz o wejściach mówi „Wejście dostępne”")
        compose
            .onNodeWithText("Zalecane: „Wejście główne”. Drzwi 100 cm. Przy wejściu jest bariera: utrudnienie.")
            .assertExists("Zdanie pod wierszem zostawia wejście główne przy równych barierach")
        assertRow(name = MAIN, marking = ACCESSIBLE, recommended = true, barrier = THRESHOLD_LINE)
        assertRow(name = SIDE, marking = ACCESSIBLE, recommended = false, barrier = "Utrudnienie: ciężkie drzwi")
        assertRow(name = YARD, marking = INACCESSIBLE, recommended = false)
        assertEquals(
            "Kolejność pozostałych wejść na liście „Wejścia” się nie zmienia",
            listOf(MAIN, SIDE, YARD),
            rowOrder(MAIN, SIDE, YARD),
        )
    }

    /**
     * Scenariusz (częściowo, przykład: jest inne): przegladanie-mapy.feature:
     *   Wejście z blokadą nie jest zalecane, a jego oznaczenie zostaje
     */
    @Test
    fun recommendsOtherIfBlocked() {
        val entrances = listOf(sampleEntrance("b"), sampleEntrance("c"))
        val barriers = listOf(entranceBarrier("b", Severity.BLOCKING, BarrierType.STAIRS_AT_ENTRANCE))
        openCard(entrances, barriers)

        compose.onNodeWithText("Wejście dostępne").assertExists("Wiersz o wejściach mówi „Wejście dostępne”")
        compose
            .onNodeWithText("Zalecane: „Wejście C”. Drzwi 100 cm.")
            .assertExists("Zdanie pod wierszem wskazuje niezablokowane wejście C")
        assertRow(name = "Wejście C", marking = ACCESSIBLE, recommended = true)
        assertRow(name = "Wejście B", marking = ACCESSIBLE, recommended = false, barrier = BLOCKED_LINE)
        assertEquals(
            "Zalecane wejście C stoi przed zablokowanym wejściem B",
            listOf("Wejście C", "Wejście B"),
            rowOrder("Wejście B", "Wejście C"),
        )
    }

    /**
     * Scenariusz (częściowo, przykład: nie ma): przegladanie-mapy.feature:
     *   Wejście z blokadą nie jest zalecane, a jego oznaczenie zostaje
     */
    @Test
    fun showsBlockedOnlyAccessible() {
        val entrances = listOf(sampleEntrance("b"), sampleEntrance("c").copy(marking = EntranceMarking.INACCESSIBLE))
        val barriers = listOf(entranceBarrier("b", Severity.BLOCKING, BarrierType.STAIRS_AT_ENTRANCE))
        openCard(entrances, barriers)

        compose
            .onNodeWithText("Brak wejścia dostępnego")
            .assertExists("Wiersz o wejściach mówi „Brak wejścia dostępnego”")
        compose
            .onNodeWithText("Jedyne wejście dostępne ma blokadę.")
            .assertExists("Zdanie pod wierszem mówi o blokadzie jedynego wejścia dostępnego")
        assertEquals(
            "Bez wejścia zalecanego nie ma przycisku „Pokaż wejście na mapie”",
            0,
            compose.onAllNodesWithText(SHOW_ENTRANCE).fetchSemanticsNodes().size,
        )
        assertRow(name = "Wejście B", marking = ACCESSIBLE, recommended = false, barrier = BLOCKED_LINE)
        assertRow(name = "Wejście C", marking = INACCESSIBLE, recommended = false)
        assertEquals(
            "Zablokowane wejście zostaje przed wejściem niedostępnym",
            listOf("Wejście B", "Wejście C"),
            rowOrder("Wejście B", "Wejście C"),
        )
    }

    private fun threeEntrances(): List<Entrance> =
        listOf(
            sampleEntrance("main").copy(name = MAIN, isMain = true, type = "main"),
            sampleEntrance("side").copy(name = SIDE),
            sampleEntrance("yard").copy(name = YARD, marking = EntranceMarking.INACCESSIBLE),
        )

    private fun openCard(entrances: List<Entrance>, barriers: List<AcceptedBarrier>) {
        val viewModel =
            BuildingCardViewModel(
                symbol = "C-13",
                repository = unitSquareCatalog("C-13"),
                networkStatus = { flowOf(true) },
                serverEntrances = ServerEntrances { entrances },
                acceptedBarriers =
                    object : AcceptedBarriers {
                        override suspend fun barriers(): List<AcceptedBarrier> = barriers

                        override fun syncedAt(): Instant = Instant.EPOCH
                    },
            )
        compose.setContent {
            DostepnyKampusTheme {
                BuildingCardScreen(
                    symbol = "C-13",
                    mode = CardMode.Expanded,
                    onShowOnMap = {},
                    onBack = {},
                    onClose = {},
                    viewModel = viewModel,
                )
            }
        }
        compose.waitUntil {
            val hasEntrances = compose.onAllNodesWithText("Wejścia").fetchSemanticsNodes().isNotEmpty()
            val hasBarriers =
                barriers.isEmpty() ||
                    compose.onAllNodesWithText("Bariery w budynku").fetchSemanticsNodes().isNotEmpty()
            hasEntrances && hasBarriers
        }
    }

    private fun assertRow(name: String, marking: String, recommended: Boolean, barrier: String? = null) {
        val texts = rowTexts(name)
        assertTrue("Wiersz „$name” ma oznaczenie „$marking”: $texts", marking in texts)
        if (recommended) {
            assertTrue("Wiersz „$name” ma napis „$RECOMMENDED”: $texts", RECOMMENDED in texts)
        } else {
            assertFalse("Wiersz „$name” nie ma napisu „$RECOMMENDED”: $texts", RECOMMENDED in texts)
        }
        if (barrier != null) {
            assertTrue("Wiersz „$name” ma barierę „$barrier”: $texts", barrier in texts)
        }
    }

    private fun entranceRow(name: String) = compose.onNode(hasClickAction() and hasText(name))

    private fun rowTexts(name: String): List<String> =
        entranceRow(name)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.Text)
            .orEmpty()
            .map { it.text }

    private fun rowTop(name: String): Float = entranceRow(name).fetchSemanticsNode().boundsInRoot.top

    private fun rowOrder(vararg names: String): List<String> = names.sortedBy(::rowTop)
}
