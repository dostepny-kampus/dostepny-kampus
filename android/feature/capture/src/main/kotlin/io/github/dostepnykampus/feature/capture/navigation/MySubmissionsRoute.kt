/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * C3 Moje zgłoszenia, bez paska nawigacji; wejście z L2 „Konto i praca” i po „Zapisz” na C2.
 *
 * Wymagania: US8.1, NFR-54
 */
@Serializable
data object MySubmissionsRoute : NavKey
