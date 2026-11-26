/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** C7 „Usunięcie bariery” zgłoszenia przyjętego [acceptedId], którego bariery już nie ma. Wymagania: US8.4 */
@Serializable
data class RemovedRoute(val acceptedId: String) : NavKey

/**
 * C7 z C3: poprawka własnego zgłoszenia „bariera usunięta” [submissionId], które czeka w telefonie.
 *
 * Wymagania: US8.3, US8.4
 */
@Serializable
data class RemovedEditRoute(val submissionId: String) : NavKey
