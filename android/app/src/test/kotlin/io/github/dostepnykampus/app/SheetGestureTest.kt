/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.app.BackGesture.Cancelled
import io.github.dostepnykampus.app.BackGesture.Completed
import io.github.dostepnykampus.app.BackGesture.Progressed
import io.github.dostepnykampus.app.BackGesture.Started
import io.github.dostepnykampus.feature.map.CardMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Arkusz V2 przy przewidywanym geście „Wstecz”: przesunięcie z postępu i stan po zdarzeniach gestu. */
class SheetGestureTest {
    /**
     * Wiersz: mała karta albo pełny ekran V2, gest „Wstecz” o postępie 0,2 i 0,6 → góra arkusza w dół proporcjonalnie
     * do postępu, a arkusz nie znika.
     *
     * Wymagania: US2
     */
    @Test
    fun us2_gesture_moves_sheet() {
        listOf(360.dp, 720.dp).forEach { height ->
            val first = sheetBackOffset(progress = 0.2f, height = height)
            val second = sheetBackOffset(progress = 0.6f, height = height)
            assertEquals(0f, sheetBackOffset(progress = 0f, height = height).value, 0.01f)
            assertTrue("przy 0,2 arkusz $height niżej niż przed gestem: $first", first > 0.dp)
            assertEquals("przy 0,6 trzy razy dalej niż przy 0,2, arkusz $height", first.value * 3, second.value, 0.01f)
            assertTrue("przy 0,6 arkusz $height wciąż widoczny: $second", second < height)
        }
    }

    /**
     * Wiersz: mała karta albo pełny ekran V2, gest „Wstecz” anulowany → arkusz jak przed gestem, V2 zostaje.
     *
     * Wymagania: US2
     */
    @Test
    fun us2_gesture_cancel_keeps_sheet() {
        listOf(false, true).forEach { isExpanded ->
            val before = SheetState(isExpanded = isExpanded)
            val moving = before.after(Started).after(Progressed(0.6f))
            assertEquals(SheetState(isExpanded = isExpanded, progress = 0.6f), moving)
            assertEquals(before, moving.after(Cancelled))
        }
    }

    /**
     * Wiersz: mała karta, „Wstecz” → arkusz zjeżdża do końca; pełny ekran po małej karcie, „Wstecz” → znowu
     * mała karta, V2 zostaje.
     *
     * Wymagania: US1.1, NFR-15, US2.8
     */
    @Test
    fun nfr15_back_collapses_closes() {
        assertEquals(SheetState(isExpanded = false, progress = 1f), SheetState().after(Completed))
        assertEquals(SheetState(isExpanded = false), SheetState(isExpanded = true).after(Completed))
    }

    /**
     * Wiersz: arkusz zwinięty → karta w trybie małej karty, rozwinięty → pełny ekran z małej karty.
     *
     * Wymagania: US2.8, NFR-53
     */
    @Test
    fun us2_8_sheet_gives_card_mode() {
        assertEquals(CardMode.Compact, SheetState().cardMode(fontScale = 1f))
        assertEquals(CardMode.Expanded, SheetState(isExpanded = true).cardMode(fontScale = 1f))
    }

    /**
     * Wiersz: V2 przy skali czcionki co najmniej 1,5 → pełny ekran od razu, także w zwiniętym arkuszu.
     *
     * Wymagania: US2.9, NFR-20
     */
    @Test
    fun nfr20_large_font_sheet_direct() {
        assertEquals(CardMode.Direct, SheetState().cardMode(fontScale = 2f))
        assertEquals(CardMode.Direct, SheetState(isExpanded = true).cardMode(fontScale = 2f))
    }

    /**
     * Wiersz: V2, gest „Wstecz” zakończony albo anulowany → arkusz dojeżdża w dół albo wraca animacją; w trakcie gestu
     * idzie za palcem bez animacji.
     *
     * Wymagania: US1.1, US2
     */
    @Test
    fun us2_slides_only_after_gesture() {
        assertTrue("dojazd w dół po zakończeniu", Completed.isSlide)
        assertTrue("powrót po anulowaniu", Cancelled.isSlide)
        assertFalse("za palcem przy postępie", Progressed(0.6f).isSlide)
        assertFalse("na początku gestu", Started.isSlide)
    }
}
