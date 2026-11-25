/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.feature.inspection.export.ExportActions
import io.github.dostepnykampus.feature.inspection.export.ExportScreen
import io.github.dostepnykampus.feature.inspection.navigation.CollectorMarkersRoute
import io.github.dostepnykampus.feature.inspection.navigation.ExportRoute
import io.github.dostepnykampus.feature.inspection.navigation.MarkersRoute
import io.github.dostepnykampus.feature.inspection.navigation.ProgressRoute
import io.github.dostepnykampus.feature.inspection.progress.CollectorMarkersScreen
import io.github.dostepnykampus.feature.inspection.progress.MarkersScreen
import io.github.dostepnykampus.feature.inspection.progress.ProgressActions
import io.github.dostepnykampus.feature.inspection.progress.ProgressScreen
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute

/** L2 → K1: „Postęp inwentaryzacji”. Wymagania: US11.1 */
fun AppNavigator.inspectionProgress() {
    leaving { add(ProgressRoute) }
}

/** L2 → K3: „Pobierz eksport”. Wymagania: US13 */
fun AppNavigator.inspectionExport() {
    leaving { add(ExportRoute) }
}

/** K1 → C5: „Otwórz” przy budynku [symbol]; działa tylko z K1. Wymagania: US11.3 */
fun AppNavigator.openMarkers(symbol: String) {
    if (current == ProgressRoute) leaving { add(MarkersRoute(symbol)) }
}

/** V2 → C5 Zbierającego: „Oznacz obejrzane piętra” na karcie budynku [symbol]; tylko z V2. Wymagania: US15.1 */
fun AppNavigator.openFloorMarking(symbol: String) {
    if (current is BuildingRoute) leaving { add(CollectorMarkersRoute(symbol)) }
}

/**
 * K1 „Postęp inwentaryzacji”, C5 „Obejrzane piętra” i K3 „Eksport” Koordynatora: „Wstecz” z K1 i K3 wraca do L2,
 * z C5 do K1, „Otwórz” przy budynku prowadzi z K1 do C5. C5 Zbierającego wraca „Wstecz” do V2.
 *
 * Wymagania: US11.1, US11.3, US13, US15.1
 */
@Composable
internal fun AppNavigator.Inspection(route: NavKey) {
    when (route) {
        is MarkersRoute -> MarkersScreen(symbol = route.buildingSymbol, onBack = { back() })
        is CollectorMarkersRoute -> CollectorMarkersScreen(symbol = route.buildingSymbol, onBack = { back() })
        ExportRoute -> ExportScreen(ExportActions(onBack = { back() }, onLeave = { backToMap() }))
        else -> ProgressScreen(ProgressActions(onBack = { back() }, onOpen = { openMarkers(it) }))
    }
}
