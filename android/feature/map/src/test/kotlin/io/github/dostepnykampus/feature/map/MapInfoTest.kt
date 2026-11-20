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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.overlay.MapInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** Przycisk „ⓘ” mapy V1 i arkusz „Legenda” ze źródłami. Wymagania: US1.4, NFR-17, NFR-18, NFR-37, NFR-40 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class MapInfoTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** „ⓘ” jest przyciskiem o celu dotyku 48 dp z nazwą dla TalkBack. Wymagania: NFR-17, NFR-18 */
    @Test
    fun namesInfoButton() {
        compose.setContent { Info(isOpenAtStart = false) }
        val button = compose.onNodeWithContentDescription(NAME, useUnmergedTree = true)
        button.assertExists()
        compose
            .onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
        compose.onNodeWithText("Legenda").assertDoesNotExist()
    }

    /**
     * Arkusz „Legenda” objaśnia każdy znak mapy słowem, także oba poziomy budynków i znaki barier; znaki wejść
     * i barier różnią się kształtem. Na końcu są „Źródła danych” z licencją i dniem stanu danych, a „Zamknij”
     * zamyka arkusz.
     *
     * Wymagania: US1.4, US16.1, NFR-21, NFR-37, NFR-40, NFR-53, US1.3
     */
    @Test
    fun showsLegendWithSources() {
        compose.setContent { Info(isOpenAtStart = false) }
        compose.onNodeWithContentDescription(NAME).performClick()
        compose.onNodeWithText("Legenda").assertExists()
        SIGNS.forEach { compose.onNodeWithText(it).assertExists() }
        compose.onNodeWithText("Źródła danych").assertExists()
        val street = "Mapa ulic i kontury budynków: © OpenStreetMap contributors, dane na otwartej licencji ODbL."
        compose.onNodeWithText(street).assertExists()
        val campus = "Wejścia i plany pięter: Źródło: Politechnika Wrocławska, stan z 18.12.2023."
        compose.onNodeWithText(campus).assertExists()
        val close = compose.onNodeWithText("Zamknij").assertHasClickAction()
        close.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)).performClick()
        compose.onNodeWithText("Legenda").assertDoesNotExist()
    }

    /** „Wstecz” przy otwartym arkuszu „Legenda” zamyka arkusz, a mapa zostaje. Wymagania: US1.4, NFR-15 */
    @Test
    fun closesLegendOnBack() {
        compose.setContent { Info(isOpenAtStart = true) }
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("Legenda").assertDoesNotExist()
        compose.onNodeWithContentDescription(NAME).assertExists()
        compose.runOnIdle { assertFalse("aktywność trwa", compose.activity.isFinishing) }
    }

    /** Źródło podkładu: OpenFreeMap i OpenMapTiles z odnośnikami do stron projektów. Wymagania: NFR-37 */
    @Test
    fun namesBasemapSource() {
        compose.setContent { Info(isOpenAtStart = true) }
        val node = compose.onNodeWithText("Podkład mapy: OpenFreeMap © OpenMapTiles.").fetchSemanticsNode()
        val text = node.config[SemanticsProperties.Text].single()
        val links = text.getLinkAnnotations(start = 0, end = text.length)
        val urls = links.map { (it.item as LinkAnnotation.Url).url }
        assertEquals("odnośniki podkładu", listOf("https://openfreemap.org/", "https://openmaptiles.org/"), urls)
    }

    /** Bez dnia w danych panel podaje samo źródło. Wymagania: NFR-40 */
    @Test
    fun showsSourcesWithoutDay() {
        compose.setContent { Info(isOpenAtStart = true, day = null) }
        compose.onNodeWithText("Wejścia i plany pięter: Źródło: Politechnika Wrocławska.").assertExists()
    }

    /**
     * „Zamknij” mieści się w arkuszu z takim samym odstępem od prawej krawędzi jak tytuł „Legenda” od lewej.
     *
     * Wymagania: NFR-17, NFR-53
     */
    @Test
    fun keepsCloseInsideSheet() {
        compose.setContent { Info(isOpenAtStart = true) }
        assertCloseMirrorsTitle()
    }

    /** „Zamknij” w legendzie ma co najmniej 48 × 48 dp. Wymagania: NFR-17 */
    @Test
    fun keepsCloseTouchTarget() {
        compose.setContent { Info(isOpenAtStart = true) }
        compose
            .onNodeWithText("Zamknij")
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
    }

    /** Przy czcionce 200% „Zamknij” też mieści się w arkuszu z odstępem tytułu. Wymagania: NFR-20, NFR-17 */
    @Test
    @Config(fontScale = 2f)
    fun keepsCloseInsideLargeFont() {
        compose.setContent { Info(isOpenAtStart = true) }
        assertCloseMirrorsTitle()
    }

    /** Zrzut arkusza „Legenda” przy czcionce 200%: cały przycisk „Zamknij” w arkuszu. Wymagania: NFR-20 */
    @Test
    @Config(fontScale = 2f)
    fun matchesLargeFontMockup() {
        compose.setContent { Info(isOpenAtStart = true) }
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut do porównania z makietą hi-fi V1 „legenda”, motyw jasny. */
    @Test
    fun matchesLightMockup() {
        compose.setContent { Info(isOpenAtStart = true) }
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut do porównania z makietą hi-fi V1 „legenda”, motyw ciemny. */
    @Test
    @Config(qualifiers = "+night")
    fun matchesDarkMockup() {
        compose.setContent { Info(isOpenAtStart = true) }
        compose.onRoot().captureRoboImage()
    }

    private fun assertCloseMirrorsTitle() {
        val root = compose.onRoot().getUnclippedBoundsInRoot()
        val title = compose.onNodeWithText("Legenda").getUnclippedBoundsInRoot()
        val close = compose.onNodeWithText("Zamknij").getUnclippedBoundsInRoot()
        val start = title.left - root.left
        val end = root.right - close.right
        assertEquals("odstęp „Zamknij” od prawej krawędzi w dp", start.value, end.value, EDGE_TOLERANCE_DP)
    }

    @Composable
    private fun Info(isOpenAtStart: Boolean, day: LocalDate? = DAY) {
        DostepnyKampusTheme {
            var isOpen by remember { mutableStateOf(isOpenAtStart) }
            Box(Modifier.fillMaxSize().background(MapGround)) {
                val corner = Modifier.align(Alignment.BottomEnd)
                MapInfo(isOpen = isOpen, dataDay = day, onOpenChange = { isOpen = it }, modifier = corner)
            }
        }
    }

    private companion object {
        val MapGround = Color(0xFFBEE3F2)
        val DAY: LocalDate = LocalDate.of(2023, 12, 18)
        const val NAME = "Legenda i źródła mapy"
        const val EDGE_TOLERANCE_DP = 0.5f
        val SIGNS =
            listOf(
                "Budynek z planem pięter",
                "Budynek bez planu",
                "Wejście dostępne",
                "Wejście niedostępne",
                "Wejście bez danych",
                "Utrudnienie",
                "Tylko z pomocą",
                "Blokada",
            )
    }
}
