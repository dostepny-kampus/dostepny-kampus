/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import io.github.dostepnykampus.feature.capture.navigation.MySubmissionsRoute
import io.github.dostepnykampus.feature.capture.navigation.SyncRoute
import io.github.dostepnykampus.feature.capture.navigation.VersionConflictRoute

/** C3 → C4: wybór wersji zgłoszenia z konfliktem wersji; poza C3 nic nie robi. Wymagania: US9.3 */
fun AppNavigator.openConflict(submissionId: String) {
    if (current == MySubmissionsRoute) leaving { add(VersionConflictRoute(submissionId)) }
}

/** C3 → C4 bez konfliktu: „Czeka na wysłanie: N”; poza C3 nic nie robi. Wymagania: US9.1 */
fun AppNavigator.openSync() {
    if (current == MySubmissionsRoute) leaving { add(SyncRoute) }
}

/** C4 → L1: „Zaloguj się” przy wygasłym zalogowaniu; poza C4 nic nie robi. Wymagania: US9.4, US14.4 */
fun AppNavigator.signInAgain() {
    if (current == SyncRoute) leaving { add(SignInRoute(isForReport = false)) }
}
