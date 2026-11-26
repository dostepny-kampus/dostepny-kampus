/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * R2 Sprawdzenie zgłoszenia, bez paska nawigacji; wejście z R1 po wzięciu zgłoszenia albo „Otwórz”.
 *
 * Wymagania: US10, US10.7
 *
 * @property submissionId identyfikator sprawdzanego zgłoszenia
 */
@Serializable
data class ReviewCheckRoute(val submissionId: String) : NavKey
