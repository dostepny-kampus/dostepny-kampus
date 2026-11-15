/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.feature.inspection.navigation.MarkersRoute
import io.github.dostepnykampus.feature.inspection.navigation.ProgressRoute
import io.github.dostepnykampus.feature.inspection.progress.MarkersScreen
import io.github.dostepnykampus.feature.inspection.progress.ProgressActions
import io.github.dostepnykampus.feature.inspection.progress.ProgressScreen

/** L2 → K1: „Postęp inwentaryzacji”. Wymagania: US11.1 */
fun AppNavigator.inspectionProgress() {
    leaving { add(ProgressRoute) }
}

/** K1 → C5: „Otwórz” przy budynku [symbol]; działa tylko z K1. Wymagania: US11.3 */
fun AppNavigator.openMarkers(symbol: String) {
    if (current == ProgressRoute) leaving { add(MarkersRoute(symbol)) }
}

/**
 * K1 „Postęp inwentaryzacji” i C5 „Obejrzane piętra” Koordynatora: „Wstecz” z K1 wraca do L2, z C5 do K1,
 * „Otwórz” przy budynku prowadzi z K1 do C5.
 *
 * Wymagania: US11.1, US11.3
 */
@Composable
internal fun AppNavigator.Inspection(route: NavKey) {
    if (route is MarkersRoute) {
        MarkersScreen(symbol = route.buildingSymbol, onBack = { back() })
    } else {
        ProgressScreen(ProgressActions(onBack = { back() }, onOpen = { openMarkers(it) }))
    }
}
