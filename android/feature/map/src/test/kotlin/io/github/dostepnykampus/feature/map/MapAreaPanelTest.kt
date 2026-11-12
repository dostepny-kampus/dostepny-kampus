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
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.feature.map.data.AreaChoice
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Panel „Teren na mapie” mapy V1: lista terenów i wybór (US1.7, NFR-18, NFR-20). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class MapAreaPanelTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val areas =
        AreaList(
            listOf(
                AreaChoice(name = "Kampus główny", buildings = 72, kilometers = null),
                AreaChoice(name = "Wittigowo", buildings = 9, kilometers = 2),
                AreaChoice(name = "T-9 Atol", buildings = 1, kilometers = 4),
            ),
        )
    private val chosen = mutableListOf<String>()
    private var closed = 0

    /**
     * Wiersz: V1, panel podaje przy każdym terenie nazwę, liczbę budynków i odległość, a pokazany teren jest
     * „wybrany” (US1.7). Po otwarciu fokus stoi na nagłówku (NFR-20).
     */
    @Test
    fun us1_7_panel_lists_areas() {
        compose.setContent { Panel() }
        compose.onNodeWithText("Teren na mapie").assertIsFocused()
        compose.onNodeWithText("72 budynki · wybrany").assertExists()
        compose.onNodeWithText("Kampus główny").assertIsSelected()
        compose.onNodeWithText("9 budynków · 2 km").assertExists()
        compose.onNodeWithText("1 budynek · 4 km").assertExists()
    }

    /** Wiersz: V1, wybranie terenu z listy zgłasza jego nazwę, a „Zamknij” zamknięcie panelu (US1.7). */
    @Test
    fun us1_7_choice_close_reported() {
        compose.setContent { Panel() }
        compose.onNodeWithText("Wittigowo").performClick()
        compose.onNodeWithText("Zamknij").performClick()
        assertEquals(listOf("Wittigowo"), chosen)
        assertEquals(1, closed)
    }

    /** Wiersz: V1, forma słowa „budynek” przy liczbie budynków terenu (US1.7). */
    @Test
    fun us1_7_buildings_word() {
        val few = listOf(2, 3, 4, 22, 72, 104).map(::buildingsWord).distinct()
        val many = listOf(0, 5, 9, 11, 12, 14, 20, 25, 112).map(::buildingsWord).distinct()
        assertEquals(listOf(R.string.mapa_teren_budynki), few)
        assertEquals(listOf(R.string.mapa_teren_budynkow), many)
        assertEquals(R.string.mapa_teren_budynek, buildingsWord(count = 1))
    }

    @Test
    fun us1_7_hifi_motyw_jasny() {
        compose.setContent { Panel() }
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun us1_7_hifi_motyw_ciemny() {
        RuntimeEnvironment.setQualifiers("+night")
        compose.setContent { Panel() }
        compose.onRoot().captureRoboImage()
    }

    @Composable
    private fun Panel() {
        DostepnyKampusTheme {
            Box(Modifier.fillMaxSize().background(Color(color = 0xFFBEE3F2))) {
                AreaPanel(
                    areas = areas,
                    onClose = { closed++ },
                    onChoose = { chosen += it },
                    modifier = Modifier.padding(Odstepy.s),
                )
            }
        }
    }
}
