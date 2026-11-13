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
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.feature.map.data.AreaNeighbor
import io.github.dostepnykampus.feature.map.data.MapEdge
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Przycisk sąsiedniego terenu mapy V1. Wymagania: US1.7, NFR-17, NFR-18 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h160dp-xhdpi")
class MapAreaNeighborTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var clicks = 0

    /**
     * Wiersz: V1, przycisk sąsiedniego terenu ma nazwę dla TalkBack „Tu kończy się teren Kampus główny. Obok:
     * Wittigowo, 2 km” i cel dotyku 48 dp; dotknięcie jest zgłoszone.
     *
     * Wymagania: US1.7, NFR-17, NFR-18
     */
    @Test
    fun us1_7_button_named_clickable() {
        compose.setContent { Button() }
        val button = compose.onNodeWithContentDescription("Tu kończy się teren Kampus główny. Obok: Wittigowo, 2 km")
        button.assertHasClickAction().assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun us1_7_hifi_motyw_jasny() {
        compose.setContent { Button() }
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun us1_7_hifi_motyw_ciemny() {
        RuntimeEnvironment.setQualifiers("+night")
        compose.setContent { Button() }
        compose.onRoot().captureRoboImage()
    }

    @Composable
    private fun Button() {
        DostepnyKampusTheme {
            Box(Modifier.fillMaxSize().background(Color(color = 0xFFBEE3F2))) {
                AreaNeighborButton(
                    from = "Kampus główny",
                    neighbor = AreaNeighbor(name = "Wittigowo", kilometers = 2, edge = MapEdge.EAST),
                    onClick = { clicks++ },
                    modifier = Modifier.padding(Odstepy.s),
                )
            }
        }
    }
}
