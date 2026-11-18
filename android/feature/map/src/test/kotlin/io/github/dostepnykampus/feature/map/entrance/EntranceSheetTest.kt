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
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.AssessmentSource
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceAssessment
import io.github.dostepnykampus.core.model.building.EntranceMarking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

private const val REPORT = "Zgłoś barierę przy wejściu"
private const val SHOW_OTHER = "Pokaż to wejście na mapie"
private const val OTHERS = "Inne wejście dostępne"
private const val NO_DATA_TEXT = "Nie mamy danych o dostępności tego wejścia."

/**
 * Arkusz wejścia na V1 po dotknięciu znaku wejścia.
 *
 * Wymagania: US2.1, US3.3, US4.6, US17.1, US17.3, BR-25, NFR-18, NFR-53
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class EntranceSheetTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val clicks = mutableListOf<String>()

    /** Wiersz: arkusz wejścia, nagłówek z fokusem, słowo oznaczenia z dopiskiem, nazwa i „Zamknij”. */
    @Test
    fun showsMainEntranceWithName() {
        val main = entrance(EntranceMarking.ACCESSIBLE, "Wejście Cesarskie", isMain = true)
        compose.setContent { Sheet(ShownEntrance(main)) }

        compose.onNodeWithText("Wejście, budynek A-1").assertIsFocused()
        compose.onNodeWithText("Dostępne, wejście główne").assertExists()
        compose.onNodeWithText("Wejście Cesarskie").assertExists()
        compose.onNodeWithText(NO_DATA_TEXT).assertDoesNotExist()
        compose.onNodeWithText("Zamknij").assertHeightIsAtLeast(48.dp).performClick()
        assertEquals("„Zamknij” zamyka arkusz", listOf("zamknij"), clicks)
    }

    /** Wiersz: wejście bez nazwy w danych nie ma nazwy, a „Niedostępne” stoi bez dopisku. Wymagania: US2.1 */
    @Test
    fun showsInaccessibleWithoutName() {
        compose.setContent { Sheet(ShownEntrance(entrance(EntranceMarking.INACCESSIBLE))) }

        compose.onNodeWithText("Niedostępne").assertExists()
        compose.onNodeWithText("wejście główne", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Wejście").assertDoesNotExist()
    }

    /** Wiersz: przy „Brak danych” zdanie, że nie mamy danych o dostępności wejścia. Wymagania: US17.1 */
    @Test
    fun showsUncheckedEntrance() {
        compose.setContent { Sheet(ShownEntrance(entrance(EntranceMarking.NO_DATA))) }

        compose.onNodeWithText("Brak danych").assertExists()
        compose.onNodeWithText(NO_DATA_TEXT).assertExists()
    }

    /** Wiersz: pod słowem oznaczenia wejścia zalecanego „Zalecane.” z datą i źródłem. Wymagania: US17.3, BR-25 */
    @Test
    fun showsRecommendedWithAssessment() {
        val assessed = entrance(EntranceMarking.ACCESSIBLE, assessment = IMPORTED)
        compose.setContent { Sheet(ShownEntrance(assessed, isRecommended = true)) }

        compose.onNodeWithText("Zalecane. Z map i opisów, stan z 18.12.2023").assertExists()
    }

    /** Wiersz: wejście zalecane bez daty i źródła w danych ma samo „Zalecane.”. Wymagania: BR-25 */
    @Test
    fun showsRecommendedWithoutDate() {
        compose.setContent { Sheet(ShownEntrance(entrance(EntranceMarking.ACCESSIBLE), isRecommended = true)) }

        compose.onNodeWithText("Zalecane.").assertExists()
    }

    /** Wiersz: wejście niezalecane ma samą datę i źródło, bez „Zalecane.”. Wymagania: US17.3 */
    @Test
    fun showsDateWithoutRecommended() {
        val checked = EntranceAssessment(LocalDate.of(2026, 11, 12), AssessmentSource.WALKTHROUGH)
        compose.setContent { Sheet(ShownEntrance(entrance(EntranceMarking.INACCESSIBLE, assessment = checked))) }

        compose.onNodeWithText("Sprawdzone na miejscu 12.11.2026").assertExists()
        compose.onNodeWithText("Zalecane", substring = true).assertDoesNotExist()
    }

    /**
     * Wiersz: arkusz wejścia zalecanego nie pokazuje innych wejść budynku ani „Pokaż to wejście na mapie”; inne
     *   wejścia są na liście „Wejścia” karty.
     *
     * Wymagania: US3.3
     */
    @Test
    fun hidesOtherEntrances() {
        val main = entrance(EntranceMarking.ACCESSIBLE, "Wejście Cesarskie", isMain = true)
        val side = entrance(EntranceMarking.ACCESSIBLE, "Wejście od ul. Norwida", id = "node/2")
        val catalog = BuildingCatalog(buildings = emptyList(), entrances = listOf(main, side), attribution = "")
        compose.setContent { Sheet(checkNotNull(catalog.shownEntrance("node/1"))) }

        compose.onNodeWithText("Zalecane.").assertExists()
        compose.onNodeWithText(OTHERS).assertDoesNotExist()
        compose.onNodeWithText("Wejście od ul. Norwida").assertDoesNotExist()
        compose.onNodeWithText(SHOW_OTHER, substring = true).assertDoesNotExist()
    }

    /**
     * Wiersz: „Zgłoś barierę przy wejściu” na końcu każdego arkusza, także przy „Brak danych”; TalkBack czyta nazwę
     *   wejścia.
     *
     * Wymagania: US4.6, NFR-18
     */
    @Test
    fun reportsBarrierAtEntrance() {
        compose.setContent { Sheet(ShownEntrance(entrance(EntranceMarking.NO_DATA, "Wejście od podwórza"))) }

        val report = hasClickAction() and hasContentDescription("$REPORT: »Wejście od podwórza«")
        val button = compose.onNode(report)
        button.performScrollTo().assertHeightIsAtLeast(48.dp).performClick()
        assertEquals("zgłoszenie przy tym wejściu", listOf("zgłoś node/1"), clicks)
    }

    /** Wiersz: przy czcionce 200% arkusz się przewija do „Zgłoś barierę przy wejściu”. Wymagania: NFR-20 */
    @Test
    @Config(fontScale = 2f)
    fun matchesLargeFontMockup() {
        compose.setContent { Sheet(recommendedMain()) }

        compose.onNodeWithText(REPORT).performScrollTo().assertExists()
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut arkusza wejścia zalecanego, motyw jasny. */
    @Test
    fun matchesLightMockup() {
        compose.setContent { Sheet(recommendedMain()) }
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut arkusza wejścia zalecanego, motyw ciemny. */
    @Test
    @Config(qualifiers = "+night")
    fun matchesDarkMockup() {
        compose.setContent { Sheet(recommendedMain()) }
        compose.onRoot().captureRoboImage()
    }

    @Composable
    private fun Sheet(shown: ShownEntrance) {
        DostepnyKampusTheme {
            Box(Modifier.fillMaxSize().background(MapGround)) {
                EntranceSheet(
                    shown = shown,
                    onClose = { clicks += "zamknij" },
                    actions = EntranceActions(onReport = { clicks += "zgłoś $it" }),
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }

    private fun recommendedMain() =
        ShownEntrance(
            entrance =
                entrance(
                    marking = EntranceMarking.ACCESSIBLE,
                    name = "Wejście Cesarskie",
                    isMain = true,
                    assessment = IMPORTED,
                ),
            isRecommended = true,
        )

    private fun entrance(
        marking: EntranceMarking,
        name: String? = null,
        isMain: Boolean = false,
        id: String = "node/1",
        assessment: EntranceAssessment? = null,
    ) = Entrance(
        id = id,
        buildingSymbol = "A-1",
        name = name,
        isMain = isMain,
        type = null,
        location = GeoPoint(longitude = 17.06, latitude = 51.107),
        marking = marking,
        assessment = assessment,
    )

    private companion object {
        val MapGround = Color(0xFFBEE3F2)
        val IMPORTED = EntranceAssessment(LocalDate.of(2023, 12, 18), AssessmentSource.IMPORT)
    }
}
