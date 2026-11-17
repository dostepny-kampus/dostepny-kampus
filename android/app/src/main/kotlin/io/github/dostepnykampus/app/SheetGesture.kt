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
import kotlin.math.abs

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
 * Wysokości osiągalne jednym gestem z tej: ona sama i sąsiednie, z niskiej także zamknięcie.
 *
 * Wymagania: US2.8, NFR-53
 */
internal fun SheetHeight.reach(): Set<SheetHeight> =
    when (this) {
        SheetHeight.Closed, SheetHeight.Low -> setOf(SheetHeight.Closed, SheetHeight.Low, SheetHeight.Half)
        SheetHeight.Half -> setOf(SheetHeight.Low, SheetHeight.Half, SheetHeight.Full)
        SheetHeight.Full -> setOf(SheetHeight.Half, SheetHeight.Full)
    }

/**
 * Wysokość, na której staje arkusz puszczony w położeniu [offset] px z prędkością [velocity] px/s, gdy przed gestem
 * stał na [settled]: przy prędkości od [flick] px/s sąsiednia w stronę rzutu, przy przesunięciu od [settled] o co
 * najmniej [step] px sąsiednia w stronę przesunięcia, inaczej [settled]. Kotwice to wysokości osiągalne jednym gestem.
 *
 * Wymagania: US2.8, NFR-53
 */
internal fun DraggableAnchors<SheetHeight>.settleTarget(
    settled: SheetHeight,
    offset: Float,
    velocity: Float,
    step: Float,
    flick: Float,
): SheetHeight {
    val shift = offset - positionOf(settled)
    val towards =
        when {
            abs(velocity) >= flick -> closestAnchor(offset, velocity > 0f)
            abs(shift) >= step -> closestAnchor(offset, shift > 0f)
            else -> null
        }
    return towards ?: settled.takeIf(::hasPositionFor) ?: closestAnchor(offset) ?: settled
}

/**
 * Kotwice arkusza w oknie wysokim na [window] px: odległość góry arkusza od góry okna dla wysokości z [reach], gdy
 * niska karta ma [low] px. Połowa okna nie stoi niżej niż niska karta.
 *
 * Wymagania: US2, US2.8
 */
internal fun sheetAnchors(
    window: Float,
    low: Float,
    reach: Set<SheetHeight> = SheetHeight.entries.toSet(),
): DraggableAnchors<SheetHeight> {
    val positions =
        mapOf(
            SheetHeight.Full to 0f,
            SheetHeight.Half to minOf(a = window / 2, b = window - low),
            SheetHeight.Low to window - low,
            SheetHeight.Closed to window,
        )
    return DraggableAnchors { positions.filterKeys { it in reach }.forEach { (height, top) -> height at top } }
}

/** Kotwice arkusza tylko z całym oknem i zamknięciem w oknie wysokim na [window] px. Wymagania: US2.9, NFR-20 */
internal fun fullOnlyAnchors(window: Float): DraggableAnchors<SheetHeight> =
    DraggableAnchors {
        SheetHeight.Full at 0f
        SheetHeight.Closed at window
    }

/** Czy arkusz przy skali czcionki [fontScale] ma tylko cały ekran i zamknięcie. Wymagania: NFR-20, US2.9 */
internal fun isFullOnlySheet(fontScale: Float): Boolean = cardModeFor(fontScale) == CardMode.Large

/**
 * Tryb karty V2 w arkuszu przy skali [fontScale]: `Large` od 1,5, inaczej z rozwinięcia.
 *
 * Wymagania: US2.8, US2.9, NFR-20
 */
internal fun SheetState.cardMode(fontScale: Float): CardMode {
    val fullScreen = cardModeFor(fontScale)
    return if (isExpanded || fullScreen == CardMode.Large) fullScreen else CardMode.Compact
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
