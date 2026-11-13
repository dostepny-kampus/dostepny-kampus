/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Przyciski mapy V1: wygląd z makiety hi-fi, cel dotyku i nazwy dla TalkBack. Wymagania: NFR-53, NFR-17, NFR-18 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class MapControlsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val start = MapCamera(center = GeoPoint(latitude = 51.107, longitude = 17.062), zoom = 16.0)

    private val names =
        listOf(
            "Przesuń mapę w górę",
            "Przesuń mapę w lewo",
            "Przesuń mapę w prawo",
            "Przesuń mapę w dół",
            "Powiększ",
            "Pomniejsz",
        )

    /**
     * Każdy przycisk mapy jest przyciskiem o celu dotyku co najmniej 48 dp z własną nazwą.
     *
     * Wymagania: NFR-17, NFR-18
     */
    @Test
    fun nfr17_nfr18_named_48dp_buttons() {
        compose.setContent { Controls {} }
        names.forEach { name ->
            compose
                .onNodeWithContentDescription(name)
                .assertHasClickAction()
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
        }
    }

    /**
     * Przyciski zmieniają widok bez gestu: „Powiększ” przybliża, strzałka w górę przesuwa na północ.
     *
     * Wymagania: NFR-53
     */
    @Test
    fun nfr53_buttons_change_camera() {
        val seen = mutableListOf<MapCamera>()
        compose.setContent { Controls(seen::add) }
        compose.onNodeWithContentDescription("Powiększ").performClick()
        compose.onNodeWithContentDescription("Przesuń mapę w górę").performClick()
        assertEquals(listOf(start.zoomedIn(), start.moved(PanDirection.UP)), seen)
    }

    /** Zrzut do porównania z makietą hi-fi V1, motyw jasny: białe koła z cieniem i szewronami. */
    @Test
    fun nfr53_hifi_motyw_jasny() {
        compose.setContent { Controls {} }
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut do porównania z makietą hi-fi V1, motyw ciemny: koła z obwódką zamiast cienia. */
    @Test
    fun nfr53_hifi_motyw_ciemny() {
        RuntimeEnvironment.setQualifiers("+night")
        compose.setContent { Controls {} }
        compose.onRoot().captureRoboImage()
    }

    @Composable
    private fun Controls(onCameraChange: (MapCamera) -> Unit) {
        DostepnyKampusTheme {
            Box(Modifier.background(MapGround).padding(Odstepy.m)) {
                MapControls(camera = start, onCameraChange = onCameraChange)
            }
        }
    }

    private companion object {
        val MapGround = Color(0xFFBEE3F2)
    }
}
