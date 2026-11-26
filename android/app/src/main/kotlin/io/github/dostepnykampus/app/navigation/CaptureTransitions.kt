/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.feature.capture.navigation.MarkingRoute
import io.github.dostepnykampus.feature.capture.navigation.MeasureRoute
import io.github.dostepnykampus.feature.capture.navigation.MySubmissionsRoute
import io.github.dostepnykampus.feature.capture.navigation.RemovedEditRoute
import io.github.dostepnykampus.feature.capture.navigation.RemovedRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportEditRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportFormRoute
import io.github.dostepnykampus.feature.capture.navigation.VersionConflictRoute
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute

/** L2 → C1: „Zmierz i zgłoś barierę”. Wymagania: US6, US14.1 */
fun AppNavigator.measure() {
    leaving { add(MeasureRoute) }
}

/** C1 → C2: „Dalej” z miejscem z pomiaru; poza C1 nic nie robi. Wymagania: US6 */
fun AppNavigator.openReportForm(route: ReportFormRoute) {
    if (current == MeasureRoute) leaving { add(route) }
}

/** L2 → C3: „Moje zgłoszenia”. Wymagania: US8.1 */
fun AppNavigator.mySubmissions() {
    leaving { add(MySubmissionsRoute) }
}

/**
 * C3 → poprawka: „Otwórz” przy zgłoszeniu z telefonu otwiera C2, przy zgłoszeniu „bariera usunięta”
 * ([isBarrierRemoved]) C7, a „Wyślij ponownie” przy nieaktualnej zmianie bariery ([isResend]) C2 nowej zmiany
 * bieżącego przyjętego zgłoszenia; poza C3 nic nie robi.
 *
 * Wymagania: US8.3, US8.4, BR-23
 */
fun AppNavigator.openEdit(submissionId: String, isBarrierRemoved: Boolean = false, isResend: Boolean = false) {
    val route =
        if (isBarrierRemoved) RemovedEditRoute(submissionId) else ReportEditRoute(submissionId, isResend = isResend)
    if (current == MySubmissionsRoute) leaving { add(route) }
}

/** C3 → C4: wybór wersji zgłoszenia z konfliktem wersji; poza C3 nic nie robi. Wymagania: US9.3 */
fun AppNavigator.openConflict(submissionId: String) {
    if (current == MySubmissionsRoute) leaving { add(VersionConflictRoute(submissionId)) }
}

/** V2 → C7: „Zgłoś, że bariery nie ma” przy zgłoszeniu przyjętym; poza V2 nic nie robi. Wymagania: US8.4 */
fun AppNavigator.openRemoved(acceptedId: String) {
    if (current is BuildingRoute) leaving { add(RemovedRoute(acceptedId)) }
}

/**
 * C6 z [route]: nową zmianę oznaczenia wejścia otwiera „Zmień oznaczenie” na V2, a zapisaną w telefonie „Otwórz”
 * albo „Wyślij ponownie” na C3; z innego ekranu nic się nie dzieje.
 *
 * Wymagania: US8.3, US17.2, BR-23, BR-24
 */
fun AppNavigator.openMarking(route: MarkingRoute) {
    val isFromHere = if (route.submissionId == null) current is BuildingRoute else current == MySubmissionsRoute
    if (isFromHere) leaving { add(route) }
}

/** V2 → C2: „Popraw zgłoszenie” przy zgłoszeniu przyjętym; poza V2 nic nie robi. Wymagania: BR-3, BR-18 */
fun AppNavigator.openReplace(acceptedId: String) {
    if (current is BuildingRoute) leaving { add(ReportEditRoute(acceptedId, isAccepted = true)) }
}

/** Poprawka C2 → C1: „Zmień miejsce”; pola czekają w szkicu, po „Dalej” wraca C2 z nowym miejscem. Wymagania: US7.4 */
fun AppNavigator.changeEditPlace() {
    if (current is ReportEditRoute) {
        leaving {
            removeAt(lastIndex)
            add(MeasureRoute)
        }
    }
}

/**
 * „Zapisz” na C2 albo „Wyślij do sprawdzenia” na C6 i C7: C1, C2, C6 i C7 znikają z historii i otwiera się C3
 * ze statusem „W telefonie”, więc „Wstecz” z C3 wraca tam, skąd zaczęła się praca (P2). Pomiar zaczęty z C3 wraca
 * do tego samego C3, bez drugiego wpisu. Poprawka z C3 też znika z historii i wraca to samo C3.
 *
 * Wymagania: NFR-54, US9.1, US8.3
 */
fun AppNavigator.reportSaved() {
    leaving {
        while (last().isCaptureRoute()) removeAt(lastIndex)
        if (last() != MySubmissionsRoute) add(MySubmissionsRoute)
    }
}

/**
 * Czy trasa należy do zbierania zgłoszenia (C1, C2, C6 i C7): od niej zależy życie szkicu zgłoszenia i zdejmowanie
 * ekranów ze stosu po zapisie. Jedyna lista takich tras; nową trasę zbierania dopisuje się tylko tutaj.
 */
internal fun NavKey.isCaptureRoute(): Boolean =
    this == MeasureRoute ||
        this is ReportFormRoute ||
        this is ReportEditRoute ||
        this is RemovedRoute ||
        this is RemovedEditRoute ||
        this is MarkingRoute
