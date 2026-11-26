/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * R1 Kolejka weryfikacji, bez paska nawigacji; wejście z L2 „Konto i praca” tylko dla Weryfikatora.
 *
 * Wymagania: US10.4
 */
@Serializable
data object ReviewQueueRoute : NavKey
