/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.feature.capture.navigation.MeasureRoute
import io.github.dostepnykampus.feature.capture.navigation.MySubmissionsRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportEditRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportFormRoute

/** L2 → C1: „Zmierz i zgłoś barierę” (US6, US14.1). */
fun AppNavigator.measure() {
    leaving { add(MeasureRoute) }
}

/** C1 → C2: „Dalej” z miejscem z pomiaru (US6); poza C1 nic nie robi. */
fun AppNavigator.openReportForm(route: ReportFormRoute) {
    if (current == MeasureRoute) leaving { add(route) }
}

/** L2 → C3: „Moje zgłoszenia” (US8.1). */
fun AppNavigator.mySubmissions() {
    leaving { add(MySubmissionsRoute) }
}

/** C3 → C2: „Otwórz” przy zgłoszeniu z telefonu otwiera jego poprawkę (US8.3); poza C3 nic nie robi. */
fun AppNavigator.openEdit(submissionId: String) {
    if (current == MySubmissionsRoute) leaving { add(ReportEditRoute(submissionId)) }
}

/** Poprawka C2 → C1: „Zmień miejsce”; pola czekają w szkicu, po „Dalej” wraca C2 z nowym miejscem (US7.4). */
fun AppNavigator.changeEditPlace() {
    if (current is ReportEditRoute) {
        leaving {
            removeAt(lastIndex)
            add(MeasureRoute)
        }
    }
}

/**
 * „Zapisz” na C2: C1 i C2 znikają z historii i otwiera się C3 ze statusem „W telefonie”, więc „Wstecz” z C3 wraca
 * tam, skąd zaczęła się praca (NFR-54, US9.1, P2). Pomiar zaczęty z C3 wraca do tego samego C3, bez drugiego wpisu.
 * Poprawka z C3 też znika z historii i wraca to samo C3 (US8.3).
 */
fun AppNavigator.reportSaved() {
    leaving {
        while (last().isCaptureStep()) removeAt(lastIndex)
        if (last() != MySubmissionsRoute) add(MySubmissionsRoute)
    }
}

private fun NavKey.isCaptureStep(): Boolean = this is ReportFormRoute || this is ReportEditRoute || this == MeasureRoute
