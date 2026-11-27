/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.grounds

import android.view.accessibility.AccessibilityManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.github.dostepnykampus.core.model.CampusBounds
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.camera.MapCamera
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

private const val NOTICE = "Pokazuję teren: Wittigowo · 2 km na południowy wschód"
private const val NOTICE_READ = "Pokazuję teren: Wittigowo, 2 km na południowy wschód"
private const val DRAG_DEGREES = 0.0005
private const val MAX_AREA_ZOOM = 17.0
private const val WORLD_DP_AT_ZOOM_0 = 512.0
private const val FULL_TURN_DEGREES = 360.0

private fun mercatorY(latitude: Double): Double = ln(tan(PI / 4 + Math.toRadians(latitude) / 2))

private fun CampusBounds.corners(): List<GeoPoint> =
    listOf(west to south, west to north, east to south, east to north).map { (longitude, latitude) ->
        GeoPoint(longitude = longitude, latitude = latitude)
    }

/**
 * Mapa V1 na prawdziwych danych PWr po wyborze terenu z listy i po wyborze budynku z innego terenu z wyszukiwania.
 *
 * Wymagania: US1.7, US1.8, NFR-56
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AreaOnMapPathTest : PwrMapFixture() {
    private fun searchT15() {
        compose.onNode(hasSetTextAction()).performTextInput("T-15")
        compose.onNodeWithContentDescription("T-15, ", substring = true).performClick()
        compose.waitForIdle()
    }

    private fun isOnMap(point: GeoPoint, view: MapCamera): Boolean {
        val bounds = compose.onNodeWithTag(MAP_TAG).fetchSemanticsNode().boundsInRoot
        val density = compose.activity.resources.displayMetrics.density
        val world = WORLD_DP_AT_ZOOM_0 * 2.0.pow(view.zoom)
        val east = (point.longitude - view.center.longitude) * world / FULL_TURN_DEGREES
        val south = (mercatorY(view.center.latitude) - mercatorY(point.latitude)) * world / (2 * PI)
        val width = (bounds.width / density).toDouble()
        val height = (bounds.height / density).toDouble()
        return width / 2 + east in 0.0..width && height / 2 + south in 0.0..height
    }

    /**
     * Wiersz: V1, wybór „Wittigowo” z listy terenów → każdy róg konturu każdego budynku terenu, w tym T-15 i P-23,
     * leży na widocznej mapie, a powiększenie nie przekracza 17.
     * Scenariusz: przegladanie-mapy.feature: Po wyborze terenu widać wszystkie jego budynki
     *
     * Wymagania: US1.7, BR-G5
     */
    @Test
    fun showsAllBuildingsOfArea() {
        showApp()
        compose.onNodeWithContentDescription("Teren: Kampus główny. Zmień teren").performClick()
        compose.onNode(hasText("Wittigowo") and isSelectable()).performClick()
        compose.waitForIdle()
        val shown = requireNotNull(camera)
        val symbols = requireNotNull(data).areas.single { it.name == "Wittigowo" }.symbols
        assertTrue("T-15 i P-23 na terenie: $symbols", symbols.containsAll(listOf("T-15", "P-23")))
        symbols.forEach { symbol ->
            requireNotNull(data).outlinesBySymbol.getValue(symbol).corners().forEach { corner ->
                assertTrue("$symbol: róg $corner poza widoczną mapą, kamera $shown", isOnMap(corner, shown))
            }
        }
        assertTrue("powiększenie ${shown.zoom} większe niż $MAX_AREA_ZOOM", shown.zoom <= MAX_AREA_ZOOM)
    }

    /**
     * Wiersz: V1 z TalkBack, wynik „T-15” z innego terenu → fokus na nagłówku karty, napis jest uprzejmym
     * komunikatem czytanym po nim; po zasłonięciu i odsłonięciu napis nie jest już komunikatem.
     * Scenariusz: przegladanie-mapy.feature: TalkBack czyta napis o zmianie terenu raz, po nagłówku karty
     *
     * Wymagania: US1.8, NFR-56
     */
    @Test
    fun readsAreaNoticeOnceAfterHeader() {
        shadowOf(compose.activity.getSystemService(AccessibilityManager::class.java)).setTouchExplorationEnabled(true)
        showApp()
        searchT15()
        compose.onNode(hasContentDescription("Budynek T-15, ", substring = true)).assertIsFocused()
        val notice = hasContentDescription(NOTICE_READ)
        val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
        compose.onNode(notice).assert(polite)
        compose.onNodeWithText("Zamknij").performClick()
        compose.onNodeWithContentDescription("Legenda i źródła mapy").performClick()
        compose.onAllNodes(notice).assertCountEquals(0)
        compose.onNodeWithText("Zamknij").performClick()
        compose.onNode(notice).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))
    }

    /**
     * Wiersz: V1, wybór wyniku „T-15” z innego terenu pokazuje napis o zmianie terenu; przesunięcie mapy gestem
     * zgłoszone przez widok mapy go gasi i napis nie wraca.
     * Scenariusz: przegladanie-mapy.feature: Ruch mapy gasi widoczny napis o zmianie terenu
     *
     * Wymagania: US1.8, NFR-23
     */
    @Test
    fun clearsAreaNoticeOnMapMove() {
        showApp()
        searchT15()
        compose.onNodeWithText(NOTICE).assertIsDisplayed()
        val view = requireNotNull(camera)
        val moved = view.copy(center = view.center.copy(longitude = view.center.longitude + DRAG_DEGREES))
        compose.runOnUiThread { idle(moved) }
        compose.waitForIdle()
        compose.onNodeWithText(NOTICE).assertDoesNotExist()
    }
}
