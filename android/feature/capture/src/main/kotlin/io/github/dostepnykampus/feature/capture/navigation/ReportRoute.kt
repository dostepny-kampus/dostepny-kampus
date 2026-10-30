/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.navigation

import androidx.navigation3.runtime.NavKey

/** V4 Zgłoszenie szybkie; [entranceId] to wejście wybrane na V2 albo V3 (US4.6), z paska dolnego brak. */
data class ReportRoute(val entranceId: String? = null) : NavKey
