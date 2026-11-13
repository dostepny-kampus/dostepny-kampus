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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** „Legenda” mapy V1: przycisk, panel ze znakami mapy i „Zamknij”. Wymagania: US1.4, NFR-17, NFR-18, NFR-40 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class MapLegendTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Przycisk „Legenda” ma cel dotyku co najmniej 48 dp i słowo dla TalkBack. Legenda objaśnia
     * każdy znak obecny na mapie i kończy się źródłem danych; znaki wejść różnią się kształtem, a każdy ma słowo.
     * „Zamknij” zwija panel.
     *
     * Wymagania: NFR-17, NFR-18, US1.4, NFR-21, NFR-40, NFR-53
     */
    @Test
    fun us1_4_legend_lists_map_signs() {
        compose.setContent { Legend(isOpenAtStart = false) }
        val words = listOf("Budynek Politechniki", "Wejście dostępne", "Wejście niedostępne", "Wejście bez danych")
        compose.onNodeWithText(words.first()).assertDoesNotExist()
        val button = compose.onNodeWithText("Legenda").assertHasClickAction().assertHeightIsAtLeast(48.dp)
        button.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)).performClick()
        words.forEach { compose.onNodeWithText(it).assertExists() }
        compose.onNodeWithText("Źródło: Politechnika Wrocławska", substring = true).assertExists()
        compose.onNodeWithText("Zamknij").assertHasClickAction().performClick()
        compose.onNodeWithText(words.first()).assertDoesNotExist()
    }

    @Test
    fun us1_4_hifi_motyw_jasny() {
        compose.setContent { Legend(isOpenAtStart = true) }
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun us1_4_hifi_motyw_ciemny() {
        RuntimeEnvironment.setQualifiers("+night")
        compose.setContent { Legend(isOpenAtStart = true) }
        compose.onRoot().captureRoboImage()
    }

    @Composable
    private fun Legend(isOpenAtStart: Boolean) {
        DostepnyKampusTheme {
            var isOpen by remember { mutableStateOf(isOpenAtStart) }
            Box(Modifier.fillMaxSize().background(Color(color = 0xFFBEE3F2))) {
                MapLegend(isOpen = isOpen, onOpenChange = { isOpen = it })
            }
        }
    }
}
