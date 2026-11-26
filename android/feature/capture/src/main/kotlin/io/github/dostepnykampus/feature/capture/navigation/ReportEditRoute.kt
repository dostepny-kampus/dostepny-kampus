/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * C2 w trybie [mode] dla zgłoszenia [submissionId].
 *
 * Wymagania: US8.3, BR-18, BR-23
 *
 * @property submissionId identyfikator własnego zgłoszenia albo zgłoszenia przyjętego
 * @property mode co C2 robi ze zgłoszeniem
 */
@Serializable
data class ReportEditRoute(val submissionId: String, val mode: ReportEditMode = ReportEditMode.OWN) : NavKey

/** Tryb C2. Wymagania: US8.3, BR-18, BR-23 */
enum class ReportEditMode {
    /** Poprawka własnego zgłoszenia z C3; zapis zastępuje to samo zgłoszenie. */
    OWN,

    /** Zmiana zgłoszenia przyjętego z V2; zapis tworzy nowe zgłoszenie z powiązaniem. */
    CHANGE_ACCEPTED,

    /** Nieaktualna zmiana z C3 wysyłana ponownie jako nowa zmiana bieżącego przyjętego zgłoszenia tej bariery. */
    RESEND_OUTDATED,
}
