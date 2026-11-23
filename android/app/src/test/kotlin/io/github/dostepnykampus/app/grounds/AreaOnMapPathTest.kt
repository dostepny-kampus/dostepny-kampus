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
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val NOTICE = "Pokazuję teren: Wittigowo · 2 km na południowy wschód"
private const val NOTICE_READ = "Pokazuję teren: Wittigowo, 2 km na południowy wschód"
private const val DRAG_DEGREES = 0.0005

/**
 * Mapa V1 na prawdziwych danych PWr po wyborze budynku z innego terenu z wyszukiwania.
 *
 * Wymagania: US1.8, NFR-56
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
