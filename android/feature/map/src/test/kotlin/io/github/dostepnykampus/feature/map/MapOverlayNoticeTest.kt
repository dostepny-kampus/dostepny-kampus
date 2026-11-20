/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.MapAreaFixtures.area
import io.github.dostepnykampus.feature.map.MapAreaFixtures.twoAreas
import io.github.dostepnykampus.feature.map.area.center
import io.github.dostepnykampus.feature.map.camera.CameraMotion
import io.github.dostepnykampus.feature.map.camera.CameraTransition
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.MapMotion
import io.github.dostepnykampus.feature.map.camera.START_ZOOM
import io.github.dostepnykampus.feature.map.overlay.MapOverlay
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Napis o zmianie terenu na nakładce mapy V1 na prostokątach testowych (nie dane PWr):
 * zaznaczony budynek „T-15” leży na terenie „Wittigowo” około 2,1 km na wschód, a mapa pokazywała „Kampus główny”.
 *
 * Wymagania: US1.8, NFR-18, NFR-23, NFR-56
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h560dp-xhdpi")
class MapOverlayNoticeTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val main = area(name = "Kampus główny", symbol = "A-1", west = 17.0)
    private val other = area(name = "Wittigowo", symbol = "T-15", west = 17.03)
    private val areas = twoAreas(main, other, symbol = "T-15")

    /**
     * Wiersz: V1, po zmianie terenu przez zaznaczony budynek nad podpisem mapy stoi napis „Pokazuję teren:
     * Wittigowo · 2 km na wschód”, który TalkBack czyta sam, z przecinkiem zamiast kropki.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Wyszukanie budynku z innego terenu zmienia teren
     *
     * Wymagania: US1.8, NFR-18, NFR-56
     */
    @Test
    fun showsNoticeAboveAttribution() {
        compose.setContent { Overlay() }
        val notice = compose.onNodeWithText(NOTICE)
        notice.assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
        notice.assert(hasContentDescription("Pokazuję teren: Wittigowo, 2 km na wschód"))
        val attribution = compose.onNodeWithText("© OpenStreetMap").getUnclippedBoundsInRoot()
        assertTrue(notice.getUnclippedBoundsInRoot().bottom <= attribution.top)
    }

    /**
     * Wiersz: V1, napis o zmianie terenu znika, gdy osoba ruszy mapę przyciskiem, bez limitu czasu.
     *
     * Wymagania: US1.8, NFR-23
     */
    @Test
    fun hidesNoticeAfterMove() {
        compose.setContent { Overlay() }
        compose.onNodeWithContentDescription("Przesuń mapę w górę").performClick()
        compose.onNodeWithText(NOTICE).assertDoesNotExist()
    }

    /**
     * Wiersz: V1, przy otwartej małej karcie V2 napis o zmianie terenu stoi nad kartą, choć przycisku terenu
     * i „ⓘ” nad kartą nie ma.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Wyszukanie budynku z innego terenu zmienia teren
     *
     * Wymagania: US1.8, US2.8, NFR-56
     */
    @Test
    fun showsNoticeAboveSmallCard() {
        compose.setContent { Overlay(hasPanels = false) }
        compose.onNodeWithText(NOTICE).assertExists()
        compose.onNodeWithContentDescription("Legenda i źródła mapy").assertDoesNotExist()
    }

    /**
     * Wiersz: V1, TalkBack czyta napis o zmianie terenu raz: po zamknięciu panelu, który go zasłaniał, napis wraca
     * bez komunikatu stanu.
     * Scenariusz (częściowo): przegladanie-mapy.feature: TalkBack czyta napis o zmianie terenu raz, po nagłówku karty
     *
     * Wymagania: US1.8, NFR-18
     */
    @Test
    fun announcesNoticeOnce() {
        compose.setContent { Overlay() }
        compose.onNodeWithContentDescription("Legenda i źródła mapy").performClick()
        compose.onNodeWithText(NOTICE).assertDoesNotExist()
        compose.onNodeWithText("Zamknij").performClick()
        compose.onNodeWithText(NOTICE).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))
    }

    /**
     * Wiersz: V1, napis o zmianie terenu pojawia się po przelocie: w czasie ruchu mapy go nie ma, po jego czasie
     * przenika.
     *
     * Wymagania: US1.8, NFR-56
     */
    @Test
    fun showsNoticeAfterFlight() {
        val motion = MapMotion()
        val flight =
            CameraTransition(
                camera = MapCamera(center = other.bounds.center, zoom = START_ZOOM),
                motion = CameraMotion.FLIGHT,
                durationMillis = FLIGHT_MILLIS,
            )
        compose.mainClock.autoAdvance = false
        compose.setContent { Overlay(motion = motion) }
        compose.runOnUiThread { motion.record(flight) }
        compose.mainClock.advanceTimeBy(FLIGHT_MILLIS / 2)
        compose.onNodeWithText(NOTICE).assertDoesNotExist()
        compose.mainClock.advanceTimeBy(FLIGHT_MILLIS)
        compose.onNodeWithText(NOTICE).assertExists()
    }

    @Test
    fun matchesLightMockup() {
        compose.setContent { Overlay() }
        compose.onRoot().captureRoboImage()
    }

    @Composable
    private fun Overlay(hasPanels: Boolean = true, motion: MapMotion? = null) {
        DostepnyKampusTheme {
            MapOverlay(
                camera = MapCamera(center = other.bounds.center, zoom = START_ZOOM),
                hasControls = true,
                dataDay = null,
                onCameraChange = {},
                areas = areas,
                onAreaShow = {},
                modifier = Modifier.fillMaxSize().background(Color(color = 0xFFBEE3F2)),
                hasPanels = hasPanels,
                hasNotice = true,
                motion = motion,
                content = {},
            )
        }
    }

    private companion object {
        const val FLIGHT_MILLIS = 1000L
        const val NOTICE = "Pokazuję teren: Wittigowo · 2 km na wschód"
    }
}
