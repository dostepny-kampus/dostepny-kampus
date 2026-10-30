/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.feature.auth.navigation.AccountRoute
import io.github.dostepnykampus.feature.auth.navigation.SignInRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportStep
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import io.github.dostepnykampus.feature.map.navigation.EntrancesRoute
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import io.github.dostepnykampus.feature.map.navigation.MapRoute

/** V1 → V2 budynku [symbol]; „Wstecz” z V2 wraca do V1 z zaznaczonym budynkiem (US1.1, US2, US3.2). */
fun AppNavigator.openBuilding(symbol: String) {
    leaving {
        this[0] = MapRoute(MapFocus.Building(symbol))
        add(BuildingRoute(symbol))
    }
}

/** V2 → V3 budynku [symbol]: „Sprawdź, którędy wejść” (US3, US3.2). */
fun AppNavigator.openEntrances(symbol: String) {
    leaving { add(EntrancesRoute(symbol)) }
}

/** V1 z zaznaczonym [focus] i wyczyszczoną historią: „Pokaż na mapie”, „Pokaż wejście na mapie” (US2.1, BR-25). */
fun AppNavigator.showOnMap(focus: MapFocus?) {
    leaving {
        subList(fromIndex = 1, toIndex = size).clear()
        this[0] = MapRoute(focus)
    }
}

/** „Zgłoś barierę”, także przy wejściu [entranceId]: V4 krok 1, a Gość najpierw L1 (US4, US4.6, US14.2). */
fun AppNavigator.report(entranceId: String? = null) {
    val route =
        if (isSignedIn) {
            ReportRoute(entranceId = entranceId)
        } else {
            SignInRoute(isForReport = true, entranceId = entranceId)
        }
    leaving { add(route) }
}

/** Następny krok V4: „Dalej” po zdjęciu, wybór grupy albo piętra (US4.1, US4.3, US4.5). */
fun AppNavigator.nextReportStep() {
    val route = (current as? ReportRoute)?.takeIf { it.step < ReportStep.SEND } ?: return
    leaving { add(route.copy(step = ReportStep.entries[route.step.ordinal + 1])) }
}

/** „Wyślij”: potwierdzenie zamiast kroków V4, więc „Wstecz” prowadzi na ekran sprzed V4 (US4.4, P2). */
fun AppNavigator.reportSent() {
    if (current !is ReportRoute) return
    leaving {
        dropReport()
        add(ReportRoute(step = ReportStep.SENT))
    }
}

/** „Tak, zrezygnuj” po „Wstecz” z kroku 1: ekran sprzed V4, zgłoszenie nie powstaje (UC7 1b). */
fun AppNavigator.leaveReport() {
    leaving { dropReport() }
}

/** Po zalogowaniu na L1: V4 krok 1 z wejściem wybranym przed L1 albo L2 (US14.2, UC7 1a, P3). */
fun AppNavigator.signedIn() {
    val route = current as? SignInRoute ?: return
    leaving {
        removeAt(lastIndex)
        add(if (route.isForReport) ReportRoute(entranceId = route.entranceId) else AccountRoute)
    }
}

private fun MutableList<NavKey>.dropReport() {
    while (last() is ReportRoute) removeAt(lastIndex)
}
