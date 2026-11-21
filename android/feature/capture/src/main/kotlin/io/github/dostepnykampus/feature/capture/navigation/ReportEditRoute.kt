/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * C2 w trybie poprawki zgłoszenia z C3 albo zmiany przyjętego zgłoszenia z V2.
 *
 * Wymagania: US8.3, BR-18
 *
 * @property submissionId identyfikator zgłoszenia z kolejki telefonu albo zgłoszenia przyjętego
 * @property isAccepted czy poprawka dotyczy zgłoszenia przyjętego i tworzy nowe zgłoszenie
 */
@Serializable
data class ReportEditRoute(val submissionId: String, val isAccepted: Boolean = false) : NavKey
