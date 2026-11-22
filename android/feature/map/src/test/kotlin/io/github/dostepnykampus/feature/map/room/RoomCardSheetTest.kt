/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.room

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.barrier.ShownBarriers
import io.github.dostepnykampus.feature.map.entrance.EntranceActions
import io.github.dostepnykampus.feature.map.room.RoomCardFixtures.ENTRANCE
import io.github.dostepnykampus.feature.map.room.RoomCardFixtures.FOUND
import io.github.dostepnykampus.feature.map.room.RoomCardFixtures.GROUND
import io.github.dostepnykampus.feature.map.room.RoomCardFixtures.SECOND
import io.github.dostepnykampus.feature.map.room.RoomCardFixtures.VIA_LIFT
import io.github.dostepnykampus.feature.map.room.RoomCardFixtures.lift
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Treść otwartej karty sali i powód braku drogi. Wymagania: US18.1, US18.2, US18.3 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-xhdpi")
class RoomCardSheetTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun showsAllStepsAndOpensBuilding() {
        val opened = mutableListOf<String>()
        show(VIA_LIFT, onBuilding = { opened.add(it) })

        val heading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
        compose.onNode(hasText("Sala 329") and heading).assertExists()
        compose.onNodeWithText("Jak dojść").assertExists()
        compose.onNodeWithText("Ok. 30 m od wejścia.", substring = true).assertExists()
        compose.onNodeWithText("ok. 85 m od windy.", substring = true).assertExists()
        compose.onNodeWithContentDescription("Budynek A-1").performClick()
        assertEquals("Otwarto inny budynek", listOf("A-1"), opened)
    }

    /**
     * Wiersz: bez windy z piętra wejścia na piętro sali karta mówi to zdaniem i ma wiersz budynku.
     * Scenariusz (częściowo): szukanie-sali.feature:
     *   Bez sali w danych albo bez drogi bez schodów aplikacja mówi to zdaniem
     */
    @Test
    fun explainsMissingLift() {
        show(RoomWay.NoLift)

        compose.onNodeWithText("Żadna winda nie łączy", substring = true).assertExists()
        compose.onNodeWithContentDescription("Budynek A-1").assertExists()
    }

    @Test
    fun largeTextCanScrollToMapAction() {
        var opens = 0
        show(VIA_LIFT, density = Density(density = 1f, fontScale = 2f), onShowMap = { opens += 1 })
        compose.onNodeWithText("Pokaż na mapie").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("Przejście do planu", 1, opens) }
    }

    @Test
    fun matchesLightMockup() {
        show(VIA_LIFT)
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun linksDetailsWithFullTarget() {
        show(VIA_LIFT) { chosen(0) }

        compose.onNodeWithText("Szczegóły wejścia").assertHeightIsAtLeast(CeleDotyku.minimum)
    }

    /** Zrzut do porównania z makietą hi-fi „m-v1-mapa-sala” z wybranym krokiem 1, motyw jasny. */
    @Test
    fun matchesSelectedStepMockup() {
        show(VIA_LIFT) { chosen(0) }
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut do porównania z makietą hi-fi „m-v1-mapa-sala” z wybranym krokiem 1, motyw ciemny. */
    @Test
    @Config(qualifiers = "+night")
    fun matchesDarkSelectedStepMockup() {
        show(VIA_LIFT) { chosen(0) }
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut karty na cały ekran przy czcionce 200%: zdanie kroku stoi pod numerem i nazwą. Wymagania: NFR-20 */
    @Test
    @Config(fontScale = 2f)
    fun matchesLargeFontStepMockup() {
        show(VIA_LIFT, onShowMap = {})
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut trybu po kolei na kroku 1 do porównania z makietą hi-fi „m-v1-mapa-sala-kroki”, motyw jasny. */
    @Test
    fun matchesStepByStepMockup() {
        show(VIA_LIFT) { started() }
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut trybu po kolei na kroku 1, motyw ciemny. */
    @Test
    @Config(qualifiers = "+night")
    fun matchesDarkStepByStepMockup() {
        show(VIA_LIFT) { started() }
        compose.onRoot().captureRoboImage()
    }

    /**
     * Wiersz: arkusz wejścia z kroku ma „‹ Sala 329” zamiast „Zamknij” i wraca do karty na tym kroku.
     * Scenariusz: szukanie-sali.feature: Arkusz wejścia z kroku wraca do tego kroku
     */
    @Test
    fun returnsFromEntranceToStep() {
        val barriers = ShownBarriers(emptyList())
        val sources = RoomSheetSources(onBuilding = {}, barriers = barriers, actions = EntranceActions())
        val card = RoomCard(found = FOUND, way = VIA_LIFT, step = 0)
        compose.setContent {
            DostepnyKampusTheme {
                RoomCardLayer(room = OpenRoom(card = card, onClose = {}, onStep = {}), sources = sources)
            }
        }

        compose.onNodeWithText("Szczegóły wejścia").performClick()
        compose.onNodeWithText("Zamknij").assertDoesNotExist()
        compose.onNodeWithContentDescription("Wróć do sali 329").performClick()
        compose.onNodeWithText("Jak dojść").assertExists()
        compose.onNodeWithText("Szczegóły wejścia").assertExists()
    }

    /**
     * Wiersz: zaraz po wyszukaniu karta ma całą listę bez wybranego kroku, a plan piętro sali.
     * Scenariusz: szukanie-sali.feature: Znaleziona sala nie zaczyna pokazu drogi
     */
    @Test
    fun startsWithWholeList() {
        var shown = RoomCard(found = FOUND, way = VIA_LIFT)
        showLayer(shown) { shown = it }

        compose.onNodeWithText("Pokaż po kolei").assertExists()
        compose.onNodeWithText("Ok. 30 m od wejścia.", substring = true).assertExists()
        compose.onNodeWithText("ok. 85 m od windy.", substring = true).assertExists()
        compose.onNodeWithText("Krok 1 z 3").assertDoesNotExist()
        compose.onNodeWithText("Szczegóły wejścia").assertDoesNotExist()
        assertEquals("Plan sali", RoomMapTarget(floor = SECOND.id, point = FOUND.room.labelPoint), shown.mapTarget())
        assertEquals("Wybrany krok po wyszukaniu", null, shown.step)
    }

    /**
     * Wiersz: „Pokaż po kolei” prowadzi od wejścia przez windę, a „Wszystkie kroki” wraca z krokiem 2.
     * Scenariusz: szukanie-sali.feature: Pokaż po kolei prowadzi od wejścia do sali
     */
    @Test
    fun leadsStepByStep() {
        var shown = RoomCard(found = FOUND, way = VIA_LIFT)
        showLayer(shown) { shown = it }

        compose.onNodeWithText("Pokaż po kolei").performClick()
        compose.onNodeWithText("Krok 1 z 3").assertExists()
        compose.onNodeWithText("ok. 85 m od windy.", substring = true).assertDoesNotExist()
        assertEquals("Wejście", RoomMapTarget(floor = GROUND.id, point = ENTRANCE.location), shown.mapTarget())
        compose.onNodeWithText("Dalej").performClick()
        compose.onNodeWithText("Krok 2 z 3").assertExists()
        assertEquals("Winda", RoomMapTarget(floor = GROUND.id, point = lift(GROUND.id).labelPoint), shown.mapTarget())
        compose.onNodeWithText("Wszystkie kroki").performClick()
        compose.onNodeWithText("Pokaż po kolei").assertExists()
        assertEquals("Lista bez kroku 2", 1 to false, shown.step to shown.isStepByStep)
    }

    /**
     * Wiersz: droga tekstem przez windę, bez windy na piętrze wejścia i z przyjętym parterem; bez rysowania trasy.
     * Scenariusz: szukanie-sali.feature: Kroki do sali podane tekstem bez rysowania trasy
     */
    @Test
    fun describesWayInText() {
        var way by mutableStateOf<RoomWay>(VIA_LIFT)
        compose.setContent {
            DostepnyKampusTheme {
                val room = OpenRoom(card = RoomCard(found = FOUND, way = way), onClose = {}, onStep = {})
                RoomCardSheet(room = room, onBuilding = {}, onShowMap = null, onDetails = {})
            }
        }
        compose.onNodeWithText("Ok. 30 m od wejścia.", substring = true).assertExists()
        compose.onNodeWithText("ok. 85 m od windy.", substring = true).assertExists()

        way = VIA_LIFT.copy(lift = null, roomMeters = 115)
        compose.onNodeWithText("ok. 115 m od wejścia.", substring = true).assertExists()
        compose.onNodeWithText("Ok. 30 m od wejścia.", substring = true).assertDoesNotExist()

        way = VIA_LIFT.copy(isFloorAssumed = true)
        compose.onNodeWithText("Parter (brak piętra w danych)", substring = true).assertExists()
    }

    @Test
    fun walksStepsAndReturnsFromLift() {
        val barriers = ShownBarriers(emptyList())
        val sources = RoomSheetSources(onBuilding = {}, barriers = barriers, actions = EntranceActions())
        compose.setContent {
            DostepnyKampusTheme {
                var card by remember { mutableStateOf(RoomCard(found = FOUND, way = VIA_LIFT)) }
                val room =
                    OpenRoom(
                        card = card,
                        onClose = {},
                        onStep = { card = card.chosen(it) },
                        onStepByStep = { card = if (it) card.started() else card.listed() },
                    )
                RoomCardLayer(room = room, sources = sources)
            }
        }

        compose.onNodeWithText("Pokaż po kolei").performClick()
        compose.onNodeWithText("Krok 1 z 3").assertExists()
        compose.onNodeWithText("ok. 85 m od windy.", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Wstecz").assertIsNotEnabled()
        compose.onNodeWithText("Dalej").performClick()
        compose.onNodeWithText("Szczegóły windy").performClick()
        compose.onNodeWithText("Jedzie na:", substring = true).assertExists()
        compose.onNodeWithContentDescription("Wróć do sali 329").performClick()

        compose.onNodeWithText("Krok 2 z 3").assertExists()
        compose.onNodeWithText("Dalej").performClick()
        compose.onNodeWithText("Dalej").assertIsNotEnabled()
        compose.onNodeWithText("Wszystkie kroki").performClick()
        compose.onNodeWithText("Pokaż po kolei").assertExists()
        compose.onNodeWithText("Ok. 30 m od wejścia.", substring = true).assertExists()
    }

    private fun showLayer(start: RoomCard, onCard: (RoomCard) -> Unit) {
        val barriers = ShownBarriers(emptyList())
        val sources = RoomSheetSources(onBuilding = {}, barriers = barriers, actions = EntranceActions())
        compose.setContent {
            DostepnyKampusTheme {
                var card by remember { mutableStateOf(start) }
                onCard(card)
                val room =
                    OpenRoom(
                        card = card,
                        onClose = {},
                        onStep = { card = card.chosen(it) },
                        onStepByStep = { card = if (it) card.started() else card.listed() },
                    )
                RoomCardLayer(room = room, sources = sources)
            }
        }
    }

    private fun show(
        way: RoomWay,
        onBuilding: (String) -> Unit = {},
        density: Density? = null,
        onShowMap: (() -> Unit)? = null,
        card: RoomCard.() -> RoomCard = { this },
    ) {
        val room = OpenRoom(card = RoomCard(found = FOUND, way = way).card(), onClose = {}, onStep = {})
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides (density ?: LocalDensity.current)) {
                DostepnyKampusTheme {
                    RoomCardSheet(room = room, onBuilding = onBuilding, onShowMap = onShowMap, onDetails = {})
                }
            }
        }
    }
}
