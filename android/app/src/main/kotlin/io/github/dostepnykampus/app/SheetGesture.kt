/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.compose.ui.unit.Dp

/**
 * Przesunięcie w dół góry arkusza V2 o wysokości [height] przy przewidywanym geście „Wstecz” o postępie
 * [progress] od 0 do 1: rośnie proporcjonalnie do postępu, a w trakcie gestu arkusz zostaje widoczny.
 */
internal fun sheetBackOffset(progress: Float, height: Dp): Dp = height * progress

/** Arkusz V2: rozwinięty na całe okno albo do połowy ([isExpanded]) i postęp gestu „Wstecz” w toku ([progress]). */
internal data class SheetState(val isExpanded: Boolean = false, val progress: Float = 0f)

/** Zdarzenie przewidywanego gestu „Wstecz” nad arkuszem V2. */
internal sealed interface BackGesture {
    /** Początek gestu. */
    data object Started : BackGesture

    /** Postęp gestu od 0 do 1. */
    data class Progressed(val progress: Float) : BackGesture

    /** Gest anulowany: V2 zostaje. */
    data object Cancelled : BackGesture

    /** Gest zakończony: V2 się zamyka. */
    data object Completed : BackGesture
}

/** Stan arkusza po zdarzeniu [gesture]; rozwinięcie arkusza gest zostawia bez zmian. */
internal fun SheetState.after(gesture: BackGesture): SheetState =
    when (gesture) {
        BackGesture.Started, BackGesture.Cancelled -> copy(progress = 0f)
        is BackGesture.Progressed -> copy(progress = gesture.progress)
        BackGesture.Completed -> copy(progress = 1f)
    }

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
