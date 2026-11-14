/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.area

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.data.AreaChoice
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Przycisk terenu mapy V1 z panelem „Teren na mapie”. Wymagania: US1.6, US1.7, NFR-17, NFR-18, NFR-20 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class MapAreaSwitchTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val areas =
        AreaList(
            listOf(
                AreaChoice(name = "Kampus główny", buildings = 72, kilometers = null),
                AreaChoice(name = "Wittigowo", buildings = 9, kilometers = 2),
            ),
        )
    private val chosen = mutableListOf<String>()

    /**
     * Wiersz: V1, przycisk terenu ma nazwę dla TalkBack „Teren: Kampus główny. Zmień teren” i cel dotyku 48 dp;
     * dotknięcie otwiera panel „Teren na mapie”.
     *
     * Wymagania: US1.6, NFR-17, NFR-18, US1.7
     */
    @Test
    fun us1_6_button_opens_panel() {
        compose.setContent { Switch(isOpenAtStart = false) }
        compose.onNodeWithText("Teren na mapie").assertDoesNotExist()
        val button = compose.onNodeWithContentDescription("Teren: Kampus główny. Zmień teren")
        button.assertHasClickAction().assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithText("Teren na mapie").assertExists()
    }

    /** Wiersz: V1, wybranie terenu z listy zgłasza jego nazwę. Wymagania: US1.7 */
    @Test
    fun us1_7_choice_reported() {
        compose.setContent { Switch(isOpenAtStart = true) }
        compose.onNodeWithText("Wittigowo").performClick()
        assertEquals(listOf("Wittigowo"), chosen)
    }

    /** Wiersz: V1, „Zamknij” zamyka panel, a fokus wraca na przycisk terenu. Wymagania: NFR-20 */
    @Test
    fun nfr20_focus_returns_to_button() {
        compose.setContent { Switch(isOpenAtStart = true) }
        compose.onNodeWithText("Zamknij").performClick()
        compose.onNodeWithText("Teren na mapie").assertDoesNotExist()
        compose.onNodeWithContentDescription("Teren: Kampus główny. Zmień teren").assertIsFocused()
    }

    @Composable
    private fun Switch(isOpenAtStart: Boolean) {
        DostepnyKampusTheme {
            var isOpen by remember { mutableStateOf(isOpenAtStart) }
            MapAreaSwitch(areas = areas, isOpen = isOpen, onOpenChange = { isOpen = it }, onChoose = { chosen += it })
        }
    }
}
