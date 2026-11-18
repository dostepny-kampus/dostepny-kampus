/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.entrance

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
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

/**
 * Część „Bariery przy tym wejściu” arkusza wejścia na V1: zgłoszenia przyjęte przy wejściu zalecanym.
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

    /** Wiersz: zalecane wejście dostaje tylko zgłoszenia przy sobie, a wejście niezalecane żadnych. */
    @Test
    fun keepsBarriersOfRecommended() {
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
        assertEquals("przy wejściu niezalecanym", emptyList<String>(), atOther)
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

        assertEquals("wysokość wiersza typu bariery", heightOf(BARRIERS), heightOf("Schody przy wejściu"))
    }

    private fun heightOf(text: String): Int {
        val node = compose.onNodeWithText(text).fetchSemanticsNode()
        return node.size.height
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
