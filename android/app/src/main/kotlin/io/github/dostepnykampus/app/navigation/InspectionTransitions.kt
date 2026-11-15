/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import androidx.compose.runtime.Composable
import io.github.dostepnykampus.feature.inspection.navigation.ProgressRoute
import io.github.dostepnykampus.feature.inspection.progress.ProgressActions
import io.github.dostepnykampus.feature.inspection.progress.ProgressScreen

/** L2 → K1: „Postęp inwentaryzacji”. Wymagania: US11.1 */
fun AppNavigator.inspectionProgress() {
    leaving { add(ProgressRoute) }
}

/**
 * K1 „Postęp inwentaryzacji”: „Wstecz” wraca do L2.
 *
 * Wymagania: US11.1
 * TODO(#1843): „Otwórz” przy budynku prowadzi do C5 Koordynatora.
 */
@Composable
internal fun AppNavigator.InspectionProgress() {
    ProgressScreen(ProgressActions(onBack = { back() }, onOpen = {}))
}
