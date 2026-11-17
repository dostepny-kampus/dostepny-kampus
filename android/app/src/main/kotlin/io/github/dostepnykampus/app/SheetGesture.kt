/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.ui.unit.Dp
import io.github.dostepnykampus.app.navigation.AppNavigator
import io.github.dostepnykampus.app.navigation.cardModeFor
import io.github.dostepnykampus.feature.map.building.CardMode
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute

/**
 * Przesunięcie w dół góry arkusza V2 o wysokości [height] przy przewidywanym geście „Wstecz” o postępie
 * [progress] od 0 do 1: rośnie proporcjonalnie do postępu, a w trakcie gestu arkusz zostaje widoczny.
 */
internal fun sheetBackOffset(progress: Float, height: Dp): Dp = height * progress

/**
 * Arkusz V2: rozwinięty na połowę albo całe okno albo mała karta o wysokości treści ([isExpanded]) i postęp gestu
 * „Wstecz” w toku ([progress]).
 */
internal data class SheetState(val isExpanded: Boolean = false, val progress: Float = 0f)

/**
 * Wysokość arkusza V2: zamknięty, niski z małą kartą, połowa okna i całe okno.
 *
 * Wymagania: US2, US2.8
 */
internal enum class SheetHeight {
    Closed,
    Low,
    Half,
    Full,
}

// TODO(#2996): połowa okna z nagłówkiem niskiej karty i bez przewijania, zamiast treści pełnego ekranu

/** Czy na tej wysokości karta pokazuje pełny opis. Wymagania: US2.8 */
internal val SheetHeight.isExpanded: Boolean
    get() = this == SheetHeight.Half || this == SheetHeight.Full

/** Wysokość o stopień wyżej; `null` na całym oknie. Wymagania: US2.8, NFR-18 */
internal fun SheetHeight.higher(): SheetHeight? =
    when (this) {
        SheetHeight.Closed, SheetHeight.Low -> SheetHeight.Half
        SheetHeight.Half -> SheetHeight.Full
        SheetHeight.Full -> null
    }

/** Wysokość o stopień niżej, bez zamknięcia; `null` na niskiej. Wymagania: US2.8, NFR-18 */
internal fun SheetHeight.lower(): SheetHeight? =
    when (this) {
        SheetHeight.Full -> SheetHeight.Half
        SheetHeight.Half -> SheetHeight.Low
        SheetHeight.Low, SheetHeight.Closed -> null
    }

/** Wysokość po dotknięciu uchwytu: następna wyższa, a z całego okna znowu niska. Wymagania: US2.8, NFR-53 */
internal fun SheetHeight.next(): SheetHeight = higher() ?: SheetHeight.Low

/**
 * Kotwice arkusza w oknie wysokim na [window] px: odległość góry arkusza od góry okna dla każdej wysokości, gdy niska
 * karta ma [low] px. Połowa okna nie stoi niżej niż niska karta.
 *
 * Wymagania: US2, US2.8
 */
internal fun sheetAnchors(window: Float, low: Float): DraggableAnchors<SheetHeight> =
    DraggableAnchors {
        SheetHeight.Full at 0f
        SheetHeight.Half at minOf(a = window / 2, b = window - low)
        SheetHeight.Low at window - low
        SheetHeight.Closed at window
    }

// TODO(#2997): przy dużej czcionce arkusz z całym oknem i zamknięciem zamiast osobnego ekranu

/**
 * Tryb karty V2 w arkuszu przy skali [fontScale]: `Direct` od 1,5, inaczej z rozwinięcia.
 *
 * Wymagania: US2.8, US2.9, NFR-20
 */
internal fun SheetState.cardMode(fontScale: Float): CardMode {
    val fullScreen = cardModeFor(fontScale)
    return if (isExpanded || fullScreen == CardMode.Direct) fullScreen else CardMode.Compact
}

/**
 * Dotknięcie pustej mapy V1: zamyka kartę V2 nad mapą, więc V1 wraca bez zaznaczenia budynku;
 * bez karty na wierzchu historia zostaje bez zmian.
 *
 * Wymagania: US2.8, NFR-17
 */
internal val closeCardOnEmptyMap: AppNavigator.() -> Unit = { if (current is BuildingRoute) back() }

/** Zdarzenie przewidywanego gestu „Wstecz” nad arkuszem V2. */
internal sealed interface BackGesture {
    /** Początek gestu. */
    data object Started : BackGesture

    /** Postęp gestu od 0 do 1. */
    data class Progressed(val progress: Float) : BackGesture

    /** Gest anulowany: V2 zostaje. */
    data object Cancelled : BackGesture

    /** Gest zakończony: pełny ekran wraca do małej karty, a mała karta zamyka V2. Wymagania: NFR-15 */
    data object Completed : BackGesture
}

/** Stan arkusza po zdarzeniu [gesture]; zakończony gest zwija pełny ekran do małej karty. Wymagania: NFR-15 */
internal fun SheetState.after(gesture: BackGesture): SheetState =
    when (gesture) {
        BackGesture.Started, BackGesture.Cancelled -> copy(progress = 0f)
        is BackGesture.Progressed -> copy(progress = gesture.progress)
        BackGesture.Completed -> if (isExpanded) SheetState() else copy(progress = 1f)
    }

/**
 * Czy zdarzenie [gesture] zamyka V2: tylko zakończony gest nad małą kartą, pełny ekran najpierw się zwija.
 *
 * Wymagania: NFR-15
 */
internal fun SheetState.closesOn(gesture: BackGesture): Boolean = gesture == BackGesture.Completed && !isExpanded

/**
 * Arkusz przechodzi do stanu po zdarzeniu gestu animacją ruchu (`true`) albo od razu, za palcem (`false`):
 * po zakończeniu dojeżdża w dół do końca, po anulowaniu wraca na swoje miejsce.
 */
internal val BackGesture.isSlide: Boolean
    get() =
        when (this) {
            BackGesture.Completed, BackGesture.Cancelled -> true
            BackGesture.Started, is BackGesture.Progressed -> false
        }
