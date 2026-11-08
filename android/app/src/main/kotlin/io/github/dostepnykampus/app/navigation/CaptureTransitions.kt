/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import io.github.dostepnykampus.feature.capture.navigation.MeasureRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportFormRoute

/** L2 → C1: „Zmierz i zgłoś barierę” (US6, US14.1). */
fun AppNavigator.measure() {
    leaving { add(MeasureRoute) }
}

/** C1 → C2: „Dalej” z miejscem z pomiaru (US6); poza C1 nic nie robi. */
fun AppNavigator.openReportForm(route: ReportFormRoute) {
    if (current == MeasureRoute) leaving { add(route) }
}

/**
 * „Zapisz” na C2: C1 i C2 znikają z historii, więc „Wstecz” wraca tam, skąd zaczęła się praca (NFR-54, P2).
 * Ekranu C3 jeszcze nie ma, więc zostaje ekran sprzed pomiaru.
 */
fun AppNavigator.reportSaved() {
    leaving { while (last() is ReportFormRoute || last() == MeasureRoute) removeAt(lastIndex) }
}
