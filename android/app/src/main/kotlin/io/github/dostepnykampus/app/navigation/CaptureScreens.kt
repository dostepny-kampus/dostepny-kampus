/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import androidx.compose.runtime.Composable
import io.github.dostepnykampus.feature.capture.mine.MySubmissionsActions
import io.github.dostepnykampus.feature.capture.mine.MySubmissionsScreen
import io.github.dostepnykampus.feature.capture.navigation.ReportEditRoute
import io.github.dostepnykampus.feature.capture.report.edit.ReportEditExits
import io.github.dostepnykampus.feature.capture.report.edit.ReportEditScreen

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
