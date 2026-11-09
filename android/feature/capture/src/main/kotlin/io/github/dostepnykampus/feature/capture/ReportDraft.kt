/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pola C2 odłożone przez „Zmień miejsce” do powrotu z C1 (US7.4). Formularz bierze je przy otwarciu z nowym
 * miejscem, a wyjście z C1 je kasuje, więc nie trafiają do następnego zgłoszenia. Tylko w pamięci procesu.
 */
@Singleton
class ReportDraft
    @Inject
    constructor() {
        private var kept: ReportUiState? = null

        /** Odkłada pola formularza. */
        fun keep(state: ReportUiState) {
            kept = state
        }

        /** Oddaje odłożone pola raz; potem schowek jest pusty. */
        fun take(): ReportUiState? = kept.also { kept = null }

        /** Kasuje odłożone pola. */
        fun clear() {
            kept = null
        }
    }
