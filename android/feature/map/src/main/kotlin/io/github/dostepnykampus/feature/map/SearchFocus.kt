/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import io.github.dostepnykampus.feature.map.camera.CampusMapView

/**
 * Fokus pola „Szukaj” widziany przez mapę: [isActive], gdy pole ma fokus, czyli klawiaturę albo wyniki pod polem.
 * [release] zdejmuje fokus przez [manager].
 *
 * Wymagania: US3.1, NFR-17
 */
@Stable
internal class SearchFocus(private val manager: FocusManager) {
    /** Czy pole „Szukaj” ma teraz fokus. */
    var isActive by mutableStateOf(false)
        private set

    /** Zapisuje, czy pole ma fokus ([isFocused]). */
    fun onField(isFocused: Boolean) {
        isActive = isFocused
    }

    /** Zdejmuje fokus z pola, więc klawiatura i wyniki się chowają. */
    fun release() = manager.clearFocus(force = true)
}

/** [SearchFocus] ekranu z bieżącym zarządcą fokusu. Wymagania: US3.1 */
@Composable
internal fun rememberSearchFocus(): SearchFocus {
    val manager = LocalFocusManager.current
    return remember(manager) { SearchFocus(manager) }
}

/**
 * Każde dotknięcie tego elementu albo jego zawartości zdejmuje fokus z pola [search], gdy zawartość już je
 * obsłużyła; przyciski i arkusze w zawartości działają od pierwszego dotknięcia.
 *
 * Wymagania: US3.1, NFR-17
 */
internal fun Modifier.releasesSearchOnTouch(search: SearchFocus): Modifier =
    pointerInput(search) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
            search.release()
        }
    }

/**
 * Ten widok mapy, w którym dotknięcie bez ruchu przy fokusie pola [search] tylko zdejmuje fokus: nie zaznacza
 * budynku ani znaku. Gest z ruchem przesuwa mapę jak zwykle, a bez fokusu pola dotknięcia przechodzą bez zmian.
 *
 * Wymagania: US3.1, NFR-17
 */
internal fun CampusMapView.closingSearchFirst(search: SearchFocus): CampusMapView {
    val view = this
    return CampusMapView { data, selection, move, inset, onCameraIdle, onBuildingsTouch, onSignTouch, modifier ->
        view.Show(
            data = data,
            selection = selection,
            move = move,
            inset = inset,
            onCameraIdle = onCameraIdle,
            onBuildingsTouch = onBuildingsTouch,
            onSignTouch = onSignTouch,
            modifier = modifier.closesSearchFirst(search),
        )
    }
}

/** Dotknięcie bez ruchu przy fokusie pola [search] tylko zdejmuje fokus. Wymagania: US3.1 */
internal fun Modifier.closesSearchFirst(search: SearchFocus): Modifier =
    pointerInput(search) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (search.isActive) {
                search.release()
                swallowTap(down)
            }
        }
    }

/**
 * Czeka na koniec dotknięcia zaczętego w [down] i zużywa jego podniesienie, gdy palec nie przesunął się dalej niż
 * próg poślizgu dotyku; gest z ruchem zostawia bez zmian.
 */
internal suspend fun AwaitPointerEventScope.swallowTap(down: PointerInputChange) {
    val slop = viewConfiguration.touchSlop
    var change = down
    while (change.pressed && (change.position - down.position).getDistance() <= slop) {
        change =
            awaitPointerEvent(PointerEventPass.Initial)
                .changes
                .firstOrNull { it.id == down.id }
                ?: return
    }
    if (!change.pressed) change.consume()
}
