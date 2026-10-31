/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * V4 Zgłoszenie szybkie (US4) na kroku [step]. Każdy krok to osobny wpis historii, więc „Wstecz” cofa o krok
 * (NFR-15). [entranceId]: wejście wybrane na V2 albo V3 przez „Zgłoś barierę przy wejściu” (US4.6).
 */
@Serializable
data class ReportRoute(val step: ReportStep = ReportStep.PHOTO, val entranceId: String? = null) : NavKey

/** Krok V4. */
enum class ReportStep {
    /** Krok 1: zdjęcie (US4.1). */
    PHOTO,

    /** Krok 2: grupa barier i piętro (US4.3, US4.5). */
    GROUP,

    /** Krok 3: wysłanie (US4.1). */
    SEND,

    /** Potwierdzenie po wysłaniu; kroki znikają wtedy z historii (US4.4, P2). */
    SENT,
}
