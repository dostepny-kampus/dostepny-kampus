/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.entrance

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.feature.map.PHOTO_URL
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

private const val BARRIERS = "Bariery przy tym wejściu"
private const val PHOTO = "Zdjęcie bariery: Schody przy wejściu"
private const val WAIT_MS = 5_000L
private val HALF_SCREEN = 400.dp

/**
 * Część „Bariery przy tym wejściu” arkusza wejścia na V1: zgłoszenia przyjęte przy każdym wejściu.
 *
 * Wymagania: US2.2, US3.3
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class EntranceBarriersTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Wiersz: przy wejściu zalecanym część „Bariery przy tym wejściu”; każde zgłoszenie ma typ, uciążliwość, zdjęcie
     *   i „Sprawdzone 14.10.2026”.
     */
    @Test
    fun showsBarriersAtRecommended() {
        usePhotoLoader(compose.activity, photoAvailable())
        compose.setContent { EntranceSheetOnMap(recommended(barrier("z1").copy(photoUrl = PHOTO_URL))) }

        compose.onNodeWithText(BARRIERS).performScrollTo().assertExists()
        listOf("Schody przy wejściu", "Blokada", "Sprawdzone 14.10.2026").forEach {
            compose.onNodeWithText(it).performScrollTo().assertExists()
        }
        compose.waitUntil(WAIT_MS) { photoShown() }
    }

    private fun photoShown() = compose.onAllNodesWithContentDescription(PHOTO).fetchSemanticsNodes().isNotEmpty()

    /** Wiersz: bez zgłoszeń przy wejściu części „Bariery przy tym wejściu” nie ma. */
    @Test
    fun hidesBarriersWithoutThem() {
        compose.setContent { EntranceSheetOnMap(recommended()) }

        compose.onNodeWithText(BARRIERS).assertDoesNotExist()
    }

    /**
     * Wiersz: każde wejście, zalecane i niezalecane, dostaje tylko zgłoszenia przy sobie.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Arkusz każdego wejścia ma te same części
     *
     * Wymagania: US2.2
     */
    @Test
    fun keepsBarriersOfEachEntrance() {
        val catalog =
            BuildingCatalog(
                buildings = emptyList(),
                entrances = listOf(entrance("node/1", EntranceMarking.ACCESSIBLE), entrance("node/2")),
                attribution = "",
            )
        val barriers = listOf(barrier("z1"), barrier("z2", entrance = "node/2"), barrier("z3", entrance = null))

        val atRecommended = checkNotNull(catalog.shownEntrance("node/1", barriers)).barriers.map { it.id }
        val atOther = checkNotNull(catalog.shownEntrance("node/2", barriers)).barriers.map { it.id }
        assertEquals("przy wejściu zalecanym", listOf("z1"), atRecommended)
        assertEquals("przy wejściu niezalecanym", listOf("z2"), atOther)
    }

    /**
     * Wiersz: arkusz z trzema barierami ze zdjęciem nie sięga wyżej niż połowa ekranu 800 dp, a jego treść
     *   przewija się do ostatniej daty.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Pinezka wejścia jest widoczna nad arkuszem wejścia
     *
     * Wymagania: US3.3, NFR-53
     */
    @Test
    fun staysWithinHalfScreen() {
        val many = recommended(barrier("z1"), barrier("z2"), barrier("z3"))
        compose.setContent { EntranceSheetOnMap(many) }

        val top = compose.onNodeWithText("Wejście, budynek A-1").getUnclippedBoundsInRoot().top
        assertTrue("nagłówek arkusza w dolnej połowie ekranu: $top", top >= HALF_SCREEN)
        compose.onAllNodesWithText("Sprawdzone 14.10.2026")[2].performScrollTo().assertIsDisplayed()
    }

    /** Zrzut arkusza wejścia zalecanego z barierą przy wejściu, motyw jasny. */
    @Test
    fun matchesLightMockup() {
        compose.setContent { EntranceSheetOnMap(recommended(barrier("z1"))) }
        compose.onRoot().captureRoboImage()
    }

    /** Wiersz: przy czcionce 200% arkusz z barierą przewija się do jej daty. Wymagania: NFR-20 */
    @Test
    @Config(fontScale = 2f)
    fun matchesLargeFontMockup() {
        compose.setContent { EntranceSheetOnMap(recommended(barrier("z1"))) }

        compose.onNodeWithText("Sprawdzone 14.10.2026").performScrollTo().assertExists()
        compose.onRoot().captureRoboImage()
    }

    /** Wiersz: typ bariery stoi krojem nagłówka części, nie większym. Wymagania: US2.2, NFR-21 */
    @Test
    fun showsTypeAsPartHeading() {
        compose.setContent { EntranceSheetOnMap(recommended(barrier("z1"))) }

        val heading = compose.onNodeWithText(BARRIERS).fetchSemanticsNode().size.height
        val type = compose.onNodeWithText("Schody przy wejściu").fetchSemanticsNode().size.height
        assertEquals("wysokość wiersza typu bariery", heading, type)
    }

    private fun recommended(vararg barriers: AcceptedBarrier) =
        ShownEntrance(
            entrance = entrance("node/1", EntranceMarking.ACCESSIBLE).copy(name = "Wejście Cesarskie"),
            isRecommended = true,
            barriers = barriers.toList(),
        )

    private fun entrance(id: String, marking: EntranceMarking = EntranceMarking.INACCESSIBLE) =
        Entrance(
            id = id,
            buildingSymbol = "A-1",
            name = null,
            isMain = id == "node/1",
            type = null,
            location = GeoPoint(longitude = 17.06, latitude = 51.107),
            marking = marking,
        )

    private fun barrier(id: String, entrance: String? = "node/1") =
        AcceptedBarrier(
            id = id,
            location = GeoPoint(longitude = 17.06, latitude = 51.107),
            severity = Severity.BLOCKING,
            type = BarrierType.STAIRS_AT_ENTRANCE,
            buildingSymbol = "A-1",
            verifiedAt = Instant.parse("2026-10-14T10:00:00Z"),
            entranceId = entrance,
        )
}
