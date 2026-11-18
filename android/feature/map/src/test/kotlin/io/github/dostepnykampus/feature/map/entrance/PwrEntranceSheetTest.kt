/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.entrance

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.AssessmentSource
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceAssessment
import io.github.dostepnykampus.core.model.building.EntranceMarking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

private const val APPROACH = "Do wejścia prowadzi pochylnia."
private const val WIDTH = "Szerokość drzwi"

/**
 * Arkusz wejścia na V1 z danymi warstwy wejść planów PWr: „Dojście” z opisu, „Drzwi” z szerokością, źródło z datą
 * i szerokość drzwi przy innych wejściach dostępnych.
 *
 * Wymagania: US2.1, US3.3, US17.3, BR-24, BR-27
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class PwrEntranceSheetTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Wiersz: wejście zalecane z planów PWr z opisem dojścia i drzwiami 120 cm → część „Dojście” z opisem i część
     *   „Drzwi” z „Szerokość drzwi: 120 cm”.
     *
     * Wymagania: US3.3, BR-27
     */
    @Test
    fun showsApproachAndDoor() {
        compose.setContent { Sheet(recommended(approach = APPROACH, doorWidthCm = 120)) }

        compose.onNode(hasText("Dojście") and isHeading()).assertExists()
        compose.onNodeWithText(APPROACH).assertExists()
        compose.onNode(hasText("Drzwi") and isHeading()).assertExists()
        compose.onNodeWithText("$WIDTH: 120 cm").assertExists()
    }

    /**
     * Wiersz: wejście zalecane bez opisu dojścia i z szerokością 0 → bez części „Dojście” i „Drzwi” i bez szerokości.
     *
     * Wymagania: US3.3, BR-27
     */
    @Test
    fun hidesEmptyParts() {
        compose.setContent { Sheet(recommended(approach = "", doorWidthCm = 0)) }

        compose.onNodeWithText("Dojście").assertDoesNotExist()
        compose.onNodeWithText("Drzwi").assertDoesNotExist()
        compose.onAllNodes(hasText(WIDTH, substring = true)).assertCountEquals(0)
    }

    /**
     * Wiersz: wejście zalecane z planów PWr z dniem edycji warstwy 12.10.2024 → „Zalecane. Z planów Politechniki
     *   Wrocławskiej, stan z 12.10.2024”.
     *
     * Wymagania: US17.3, BR-24, BR-27
     */
    @Test
    fun namesPwrSource() {
        compose.setContent { Sheet(recommended(approach = APPROACH, doorWidthCm = 120)) }

        compose.onNodeWithText("Zalecane. Z planów Politechniki Wrocławskiej, stan z 12.10.2024").assertExists()
    }

    /**
     * Wiersz: w „Inne wejście dostępne” szerokość drzwi przy każdym wejściu, które ją ma w danych; wejście
     *   z szerokością 0 stoi bez szerokości.
     *
     * Wymagania: US3.3, BR-27
     */
    @Test
    fun showsOthersDoorWidth() {
        compose.setContent { Sheet(recommended(approach = "", doorWidthCm = 0)) }

        compose.onNodeWithText("$WIDTH: 90 cm").performScrollTo().assertExists()
        compose.onAllNodes(hasText(WIDTH, substring = true)).assertCountEquals(1)
    }

    /**
     * Wiersz: wejście niezalecane z opisem i drzwiami 90 cm → szerokość drzwi i opis bez części „Dojście” i „Drzwi”.
     *
     * Wymagania: US2.1, BR-27
     */
    @Test
    fun showsPlainFactsWhenNotRecommended() {
        val plain = pwr(id = "pwr/4", name = "Wejście", approach = APPROACH, doorWidthCm = 90)
        compose.setContent { Sheet(ShownEntrance(plain)) }

        compose.onNodeWithText("$WIDTH: 90 cm").assertExists()
        compose.onNodeWithText(APPROACH).assertExists()
        compose.onNodeWithText("Dojście").assertDoesNotExist()
        compose.onNodeWithText("Drzwi").assertDoesNotExist()
    }

    /** Zrzut arkusza wejścia zalecanego z planów PWr, motyw jasny. */
    @Test
    fun matchesLightMockup() {
        compose.setContent { Sheet(recommended(approach = APPROACH, doorWidthCm = 120)) }
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut arkusza wejścia zalecanego z planów PWr przy czcionce 200%. */
    @Test
    @Config(fontScale = 2f)
    fun matchesLargeFontMockup() {
        compose.setContent { Sheet(recommended(approach = APPROACH, doorWidthCm = 120)) }
        compose.onRoot().captureRoboImage()
    }

    @Composable
    private fun Sheet(shown: ShownEntrance) {
        DostepnyKampusTheme {
            Box(Modifier.fillMaxSize().background(MapGround)) {
                EntranceSheet(
                    shown = shown,
                    onClose = {},
                    actions = EntranceActions(),
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }

    private companion object {
        val MapGround = Color(0xFFBEE3F2)

        fun recommended(approach: String, doorWidthCm: Int): ShownEntrance =
            ShownEntrance(
                entrance = pwr("pwr/1", "Wejście Cesarskie", approach, doorWidthCm),
                isRecommended = true,
                others =
                    listOf(
                        pwr(id = "pwr/2", name = "Wejście \"C\"", approach = "", doorWidthCm = 90),
                        pwr(id = "pwr/3", name = "Wejście", approach = "", doorWidthCm = 0),
                    ),
            )

        fun pwr(id: String, name: String, approach: String, doorWidthCm: Int): Entrance =
            Entrance(
                id = id,
                buildingSymbol = "A-1",
                name = name,
                isMain = false,
                type = null,
                location = GeoPoint(longitude = 17.06, latitude = 51.107),
                marking = EntranceMarking.ACCESSIBLE,
                assessment = EntranceAssessment(LocalDate.of(2024, 10, 12), AssessmentSource.PWR_PLANS),
                approach = approach,
                doorWidthCm = doorWidthCm,
            )
    }
}
