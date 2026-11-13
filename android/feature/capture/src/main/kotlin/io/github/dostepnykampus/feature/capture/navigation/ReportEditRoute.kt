/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * C2 w trybie poprawki zgłoszenia z C3.
 *
 * Wymagania: US8.3
 *
 * @property submissionId identyfikator zgłoszenia z kolejki telefonu
 */
@Serializable
data class ReportEditRoute(val submissionId: String) : NavKey
