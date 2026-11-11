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
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** „Legenda” mapy V1: przycisk, panel ze znakami mapy i „Zamknij” (US1.4, NFR-17, NFR-18, NFR-40). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class MapLegendTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** Przycisk „Legenda” jest przyciskiem o celu dotyku co najmniej 48 dp ze słowem dla TalkBack (NFR-17, NFR-18). */
    @Test
    fun nfr17_nfr18_legend_button() {
        compose.setContent { Legend(isOpenAtStart = false) }
        compose
            .onNodeWithText("Legenda")
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHeightIsAtLeast(48.dp)
        compose.onNodeWithText("Budynek Politechniki").assertDoesNotExist()
    }

    /**
     * Legenda objaśnia każdy znak obecny na mapie i kończy się źródłem danych; znaki wejść różnią się kształtem,
     * a każdy ma słowo (US1.4, NFR-21, NFR-40). „Zamknij” zwija panel (NFR-53).
     */
    @Test
    fun us1_4_legend_lists_map_signs() {
        compose.setContent { Legend(isOpenAtStart = false) }
        compose.onNodeWithText("Legenda").performClick()
        val words = listOf("Budynek Politechniki", "Wejście dostępne", "Wejście niedostępne", "Wejście bez danych")
        words.forEach { compose.onNodeWithText(it).assertExists() }
        compose.onNodeWithText("Źródło: Politechnika Wrocławska", substring = true).assertExists()
        assertEquals(2, compose.onAllNodesWithText("Legenda").fetchSemanticsNodes().size)
        compose.onNodeWithText("Zamknij").assertHasClickAction().performClick()
        compose.onNodeWithText("Budynek Politechniki").assertDoesNotExist()
    }

    /** Zrzut do porównania z makietą hi-fi V1 „legenda”, motyw jasny. */
    @Test
    fun us1_4_hifi_motyw_jasny() {
        compose.setContent { Legend(isOpenAtStart = true) }
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut do porównania z makietą hi-fi V1 „legenda”, motyw ciemny. */
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
            Box(Modifier.fillMaxSize().background(MapGround)) {
                MapLegend(isOpen = isOpen, onOpenChange = { isOpen = it })
            }
        }
    }

    private companion object {
        val MapGround = Color(0xFFBEE3F2)
    }
}
