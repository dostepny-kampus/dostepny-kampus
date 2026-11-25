/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.feature.capture.measure.MeasureScreen
import io.github.dostepnykampus.feature.capture.mine.MySubmissionsActions
import io.github.dostepnykampus.feature.capture.mine.MySubmissionsScreen
import io.github.dostepnykampus.feature.capture.navigation.MarkingRoute
import io.github.dostepnykampus.feature.capture.navigation.MeasureRoute
import io.github.dostepnykampus.feature.capture.navigation.MySubmissionsRoute
import io.github.dostepnykampus.feature.capture.navigation.RemovedRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportEditRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportFormRoute
import io.github.dostepnykampus.feature.capture.report.ReportEditExits
import io.github.dostepnykampus.feature.capture.report.ReportFormScreen
import io.github.dostepnykampus.feature.capture.report.edit.ReportEditScreen
import io.github.dostepnykampus.feature.capture.report.marking.MarkingScreen
import io.github.dostepnykampus.feature.capture.report.removed.RemovedScreen

/** Trasy pomiaru, formularza i własnych zgłoszeń. */
@Composable
internal fun AppNavigator.Capture(route: NavKey) {
    when (route) {
        MeasureRoute -> MeasureScreen(onBack = { back() }, onNext = { openReportForm(it) })

        is ReportFormRoute ->
            ReportFormScreen(
                route = route,
                onLeave = { back() },
                onSave = { reportSaved() },
                draft = reportDraft,
            )

        is ReportEditRoute -> ReportEdit(route)

        is RemovedRoute -> Removed(route)

        is MarkingRoute -> Marking(route)

        MySubmissionsRoute -> MySubmissions()

        else -> Unit
    }
}

/** C3 „Moje zgłoszenia”: „Otwórz” prowadzi do poprawki w C2, „Zmierz i zgłoś barierę” do C1. Wymagania: US8.1, US8.3 */
@Composable
internal fun AppNavigator.MySubmissions() {
    MySubmissionsScreen(MySubmissionsActions(onBack = { back() }, onMeasure = { measure() }, onOpen = { openEdit(it) }))
}

/**
 * Poprawka C2 zgłoszenia [route] z C3; zapis i wyjście wracają do C3, „Zmień miejsce” idzie przez C1.
 *
 * Wymagania: US8.3
 */
@Composable
internal fun AppNavigator.ReportEdit(route: ReportEditRoute) {
    ReportEditScreen(
        route = route,
        exits =
            ReportEditExits(
                onLeave = { back() },
                onSave = { reportSaved() },
                onChangePlace = { changeEditPlace() },
            ),
        draft = reportDraft,
    )
}

/** C7 zgłoszenia [route] z V2; „Wyślij do sprawdzenia” otwiera C3 bez C7 w historii. Wymagania: US8.4, NFR-54 */
@Composable
internal fun AppNavigator.Removed(route: RemovedRoute) {
    RemovedScreen(route = route, onLeave = { back() }, onFinish = { reportSaved() })
}

/** C6 wejścia z [route] z V2; „Wyślij do sprawdzenia” otwiera C3 bez C6 w historii. Wymagania: US17.2, NFR-54 */
@Composable
internal fun AppNavigator.Marking(route: MarkingRoute) {
    MarkingScreen(route = route, onLeave = { back() }, onFinish = { reportSaved() })
}
