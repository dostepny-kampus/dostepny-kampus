/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.data.barriers.AcceptedBarrier
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.BarrierType
import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.Severity
import io.github.dostepnykampus.core.model.SubmissionFloor
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

/** Arkusz bariery na V1 po dotknięciu znaku (US1.5, NFR-21, NFR-53). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class BarrierSheetTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val clicks = mutableListOf<String>()

    /** Wiersz: arkusz bariery, typ, uciążliwość, miejsce, data sprawdzenia i przyciski (US1.5, PW-5). */
    @Test
    fun us1_5_barrier_in_building() {
        compose.setContent { Sheet(inBuilding()) }

        val place = "A-1 Gmach Główny · Wejście Cesarskie, parter"
        val texts = listOf("Blokada", "Schody przy wejściu", place, "Sprawdzone 14.10.2026")
        texts.forEach { compose.onNodeWithText(it).assertExists() }
        compose.onNodeWithText("Zamknij").assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithText("Pokaż opis budynku").performClick()
        assertEquals(listOf("zamknij", "A-1"), clicks)
    }

    /** Wiersz: bariera poza budynkiem ma „Poza budynkiem” i nie ma „Pokaż opis budynku” (US1.5, BR-G2). */
    @Test
    fun us1_5_barrier_outside() {
        val barrier = barrier(type = null, symbol = null, floor = null).copy(verifiedAt = null)
        compose.setContent { Sheet(ChosenBarrier(barrier = barrier, building = null, entrance = null)) }

        listOf("Bariera", "Poza budynkiem").forEach { compose.onNodeWithText(it).assertExists() }
        compose.onNodeWithText("Pokaż opis budynku").assertDoesNotExist()
        compose.onNodeWithText("Sprawdzone", substring = true).assertDoesNotExist()
    }

    /** Wiersz: przy czcionce 200% arkusz się przewija, a przycisk budynku jest osiągalny (NFR-20). */
    @Test
    @Config(fontScale = 2f)
    fun nfr20_hifi_czcionka_200() {
        compose.setContent { Sheet(inBuilding()) }

        compose.onNodeWithText("Pokaż opis budynku").performScrollTo().assertExists()
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut do porównania z makietą hi-fi „m-v1-mapa-bariera”, motyw jasny. */
    @Test
    fun us1_5_hifi_motyw_jasny() {
        compose.setContent { Sheet(inBuilding()) }
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut do porównania z makietą hi-fi „m-v1-mapa-bariera”, motyw ciemny. */
    @Test
    fun us1_5_hifi_motyw_ciemny() {
        RuntimeEnvironment.setQualifiers("+night")
        compose.setContent { Sheet(inBuilding()) }
        compose.onRoot().captureRoboImage()
    }

    @Composable
    private fun Sheet(chosen: ChosenBarrier) {
        DostepnyKampusTheme {
            Box(Modifier.fillMaxSize().background(MapGround)) {
                BarrierSheet(
                    chosen = chosen,
                    onClose = { clicks += "zamknij" },
                    onBuildingClick = { clicks += it.symbol },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }

    private fun inBuilding(): ChosenBarrier {
        val ground = SubmissionFloor(number = 0)
        val barrier = barrier(type = BarrierType.STAIRS_AT_ENTRANCE, symbol = "A-1", floor = ground)
        val building = Building(symbol = "A-1", name = "Gmach Główny", outline = OUTLINE)
        return ChosenBarrier(barrier = barrier, building = building, entrance = "Wejście Cesarskie")
    }

    private fun barrier(type: BarrierType?, symbol: String?, floor: SubmissionFloor?) =
        AcceptedBarrier(
            id = "z1",
            location = GeoPoint(longitude = 17.06, latitude = 51.107),
            severity = Severity.BLOCKING,
            type = type,
            buildingSymbol = symbol,
            verifiedAt = Instant.parse("2026-10-14T10:00:00Z"),
            floor = floor,
        )

    private companion object {
        val MapGround = Color(0xFFBEE3F2)
        val CORNERS = listOf(0.0 to 0.0, 1.0 to 0.0, 1.0 to 1.0, 0.0 to 0.0)
        val OUTLINE = GeoPolygon(listOf(CORNERS.map { (lon, lat) -> GeoPoint(longitude = lon, latitude = lat) }))
    }
}
