/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.scenario

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.feature.map.PHOTO_URL
import io.github.dostepnykampus.feature.map.building.BuildingCardContent
import io.github.dostepnykampus.feature.map.building.CardMode
import io.github.dostepnykampus.feature.map.building.entranceBarrier
import io.github.dostepnykampus.feature.map.building.readyCard
import io.github.dostepnykampus.feature.map.building.sampleEntrance
import io.github.dostepnykampus.feature.map.entrance.EntranceLift
import io.github.dostepnykampus.feature.map.photoAvailable
import io.github.dostepnykampus.feature.map.usePhotoLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

private const val ROW_GAP = 48f
private const val SHOW_ENTRANCE = "Pokaż wejście na mapie"
private const val DOOR = "Szerokość drzwi: 120 cm"
private const val PLACE = "Od strony C-20, od północy."
private const val RAMP = "Do wejścia prowadzi rampa."
private val PARTS = listOf("Dojście", "Drzwi", "Bariery przy tym wejściu")
private val SHEET_EDGE = 32.dp

/**
 * Wejścia od karty budynku do arkusza wejścia: bariera przy wejściu zalecanym, opis wejścia, te same części przy
 * każdym wejściu, pinezka nad arkuszem i wejście bez nazwy w danych.
 *
 * Wymagania: US2.1, US2.2, US3.3, BR-25, BR-27, NFR-53
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp")
class EntranceCardFlowTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val flow = CardToMap(compose, symbol = "A-1")

    private val recommended =
        serverEntrance(
            id = "a1/boczne",
            symbol = "A-1",
            name = "Wejście Cesarskie",
            marking = EntranceMarking.ACCESSIBLE,
        ).copy(approach = "Podjazd od parkingu.", doorWidthCm = 120)

    private val other =
        serverEntrance(
            id = "a1/glowne",
            symbol = "A-1",
            name = "Wejście główne",
            marking = EntranceMarking.INACCESSIBLE,
        ).copy(approach = "Do wejścia prowadzą schody.", doorWidthCm = 90)

    /**
     * Scenariusz: przegladanie-mapy.feature: Opis wejścia zalecanego podaje dojście, drzwi i najbliższą windę.
     */
    @Test
    fun describesRecommendedEntrance() {
        val lift = EntranceLift(entranceId = recommended.id, meters = 30, floors = listOf("Parter", "1 piętro"))
        flow.show(entrances = listOf(other, recommended), lift = lift)
        flow.tap(hasContentDescription(SHOW_ENTRANCE, substring = true))

        assertEquals("na mapie zaznaczone wejście zalecane", recommended.id, flow.pinned?.id)
        compose.onNode(hasText("Dojście") and isHeading()).assertExists("część „Dojście”")
        compose.onNodeWithText("Podjazd od parkingu.").assertExists("opis dojścia do drzwi")
        compose.onNodeWithText(DOOR).assertExists("szerokość drzwi")
        compose.onNodeWithText("Winda, ok. 30 m od wejścia.").assertExists("najbliższa winda z odległością")
        compose.onNodeWithText("Wejście główne").assertDoesNotExist()
        compose.onNodeWithText("Do wejścia prowadzą schody.").assertDoesNotExist()
    }

    /**
     * Scenariusz: przegladanie-mapy.feature: Arkusz każdego wejścia ma te same części.
     */
    @Test
    fun givesEveryEntranceSameParts() {
        val barriers = listOf(recommended.id, other.id).map { barrier(entrance = it) }
        flow.show(entrances = listOf(other, recommended), barriers = barriers)
        flow.tap(hasContentDescription(SHOW_ENTRANCE, substring = true))
        val atRecommended = headings()
        flow.backToCard()
        flow.tap(hasText("Wejście główne"))

        assertTrue("części arkusza wejścia zalecanego: $atRecommended", atRecommended.containsAll(PARTS))
        assertEquals("te same części przy wejściu niezalecanym", atRecommended, headings())
    }

    /**
     * Scenariusz: przegladanie-mapy.feature: Pinezka wejścia jest widoczna nad arkuszem wejścia.
     */
    @Test
    fun centersPinAboveSheet() {
        usePhotoLoader(compose.activity, photoAvailable())
        val photographed = barrier(entrance = recommended.id, photoUrl = PHOTO_URL)
        flow.show(entrances = listOf(other, recommended), barriers = listOf(photographed))
        flow.tap(hasContentDescription(SHOW_ENTRANCE, substring = true))

        val header = compose.onNodeWithText("Wejście").getUnclippedBoundsInRoot()
        val root = compose.onRoot().getUnclippedBoundsInRoot()
        val (move, inset) = checkNotNull(flow.camera)
        val covered = (inset.bottom / compose.activity.resources.displayMetrics.density).dp
        assertEquals("kamera na pinezce wejścia zalecanego", recommended.location, move.camera.center)
        assertTrue("dół kamery zasłania arkusz: $covered", covered >= root.bottom - header.top)
        assertTrue("najwyżej o odstępy arkusza więcej: $covered", covered <= root.bottom - header.top + SHEET_EDGE)
        assertTrue("arkusz najwyżej do połowy ekranu: ${header.top}", header.top >= root.bottom / 2)
        compose.onNode(hasScrollAction()).assertExists("treść arkusza się przewija")
        compose.onNodeWithText("Sprawdzone 14.10.2026").performScrollTo().assertIsDisplayed()
    }

    /**
     * Scenariusz: przegladanie-mapy.feature: Wejście bez nazwy w danych ma na karcie opis położenia pod nazwą.
     */
    @Test
    fun carriesPlaceFromCardToSheet() {
        val unnamed =
            serverEntrance(
                id = "pwr/1",
                symbol = "C-19",
                name = "Wejście",
                marking = EntranceMarking.ACCESSIBLE,
            ).copy(approach = RAMP, doorWidthCm = 100, isNameGenerated = true, placeDescription = PLACE)
        val card = CardToMap(compose, symbol = "C-19")
        card.show(listOf(unnamed))
        val sentence = hasText("Zalecane wejście od strony C-20, od północy. Drzwi 100 cm.")
        compose.waitUntil { compose.onAllNodes(sentence).fetchSemanticsNodes().isNotEmpty() }

        val order = listOf("Wejście", PLACE, RAMP).map { tops(it).first() }
        assertTrue("pod nazwą najpierw położenie, potem uwaga uczelni: $order", order == order.sorted())
        card.tap(hasText("Wejście"))

        assertEquals("podpis wejścia na mapie", "Wejście", card.pinned?.label)
        compose.onNodeWithText("$PLACE\n$RAMP").assertExists("część „Dojście” z obu opisami w tej kolejności")
    }

    /**
     * Scenariusz: przegladanie-mapy.feature: Bariera przy wejściu zalecanym jest na karcie przy tym wejściu.
     */
    @Test
    fun keepsRecommendedWithBarrier() {
        val heavyDoor = entranceBarrier("b", Severity.ONLY_WITH_HELP, BarrierType.HEAVY_DOOR)
        val entrances = listOf(sampleEntrance("b"), sampleEntrance("c").copy(marking = EntranceMarking.INACCESSIBLE))
        val card = readyCard(symbol = "C-13", entrances = entrances, barriers = listOf(heavyDoor))
        compose.setContent {
            DostepnyKampusTheme {
                BuildingCardContent(
                    symbol = "C-13",
                    mode = CardMode.Expanded,
                    card = card,
                    isOnline = true,
                    onShowOnMap = {},
                    onBack = {},
                    onClose = {},
                    onReport = {},
                    onRetry = {},
                )
            }
        }

        compose.onNode(hasText("Wejście dostępne")).assertExists("wiersz o wejściach")
        compose.onNode(hasText("Wejście B") and hasText("Zalecane")).assertExists("wejście z barierą zalecane")
        val sentence = "Zalecane: „Wejście B”. Drzwi 100 cm. Przy wejściu jest bariera: tylko z pomocą."
        compose.onNode(hasText(sentence)).assertExists("zdanie pod wierszem o wejściach")
        val barrierTop = tops("Tylko z pomocą: ciężkie drzwi").single()
        val widthAbove = tops("Szerokość drzwi: 100 cm").filter { it < barrierTop }.max()
        assertTrue("bariera zaraz pod szerokością drzwi: $widthAbove, $barrierTop", barrierTop - widthAbove < ROW_GAP)
    }

    private fun tops(text: String) =
        compose.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().map { it.boundsInRoot.top }

    private fun headings(): List<String> =
        compose
            .onAllNodes(isHeading())
            .fetchSemanticsNodes()
            .flatMap { node -> node.config.getOrNull(SemanticsProperties.Text).orEmpty() }
            .map { it.text }

    private fun barrier(entrance: String, photoUrl: String? = null) =
        AcceptedBarrier(
            id = "z-$entrance",
            location = GeoPoint(longitude = 0.3, latitude = 0.4),
            severity = Severity.HINDRANCE,
            type = BarrierType.STAIRS_AT_ENTRANCE,
            buildingSymbol = "A-1",
            verifiedAt = Instant.parse("2026-10-14T10:00:00Z"),
            entranceId = entrance,
            photoUrl = photoUrl,
        )
}
