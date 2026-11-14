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
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.controls.MapInfo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** Przycisk „ⓘ” mapy V1 i panel „Źródła danych”. Wymagania: NFR-17, NFR-18, NFR-37, NFR-40 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class MapInfoTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** „ⓘ” jest przyciskiem o celu dotyku 48 dp z nazwą „Informacje o mapie” dla TalkBack. Wymagania: NFR-17, NFR-18 */
    @Test
    fun nfr17_nfr18_info_button() {
        compose.setContent { Info(isOpenAtStart = false) }
        val button = compose.onNodeWithContentDescription("Informacje o mapie", useUnmergedTree = true)
        button.assertExists()
        compose
            .onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
        compose.onNodeWithText("Źródła danych").assertDoesNotExist()
    }

    /** Panel podaje źródła, licencję i dzień stanu danych, a „Zamknij” go zamyka. Wymagania: NFR-37, NFR-40, NFR-53 */
    @Test
    fun nfr37_nfr40_sources_panel() {
        compose.setContent { Info(isOpenAtStart = false) }
        compose.onNodeWithContentDescription("Informacje o mapie").performClick()
        compose.onNodeWithText("Źródła danych").assertExists()
        val street = "Mapa ulic i kontury budynków: © OpenStreetMap contributors, dane na otwartej licencji ODbL."
        compose.onNodeWithText(street).assertExists()
        val campus = "Wejścia i plany pięter: Źródło: Politechnika Wrocławska, stan z 18.12.2023."
        compose.onNodeWithText(campus).assertExists()
        compose.onNodeWithText("Zamknij").assertHasClickAction().performClick()
        compose.onNodeWithText("Źródła danych").assertDoesNotExist()
    }

    /** Bez dnia w danych panel podaje samo źródło. Wymagania: NFR-40 */
    @Test
    fun nfr40_sources_without_day() {
        compose.setContent { Info(isOpenAtStart = true, day = null) }
        compose.onNodeWithText("Wejścia i plany pięter: Źródło: Politechnika Wrocławska.").assertExists()
    }

    /** Zrzut do porównania z makietą hi-fi V1 „źródła danych”, motyw jasny. */
    @Test
    fun nfr37_hifi_motyw_jasny() {
        compose.setContent { Info(isOpenAtStart = true) }
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut do porównania z makietą hi-fi V1 „źródła danych”, motyw ciemny. */
    @Test
    fun nfr37_hifi_motyw_ciemny() {
        RuntimeEnvironment.setQualifiers("+night")
        compose.setContent { Info(isOpenAtStart = true) }
        compose.onRoot().captureRoboImage()
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
    }
}
