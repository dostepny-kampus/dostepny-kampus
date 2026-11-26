/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** C4 przy konflikcie wersji zgłoszenia [submissionId]. Wymagania: US9.3 */
@Serializable
data class VersionConflictRoute(val submissionId: String) : NavKey
