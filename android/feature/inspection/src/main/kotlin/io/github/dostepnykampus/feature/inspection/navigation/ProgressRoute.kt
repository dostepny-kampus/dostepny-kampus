/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * K1 Postęp inwentaryzacji, bez paska nawigacji; wejście z L2 „Konto i praca” tylko dla Koordynatora.
 *
 * Wymagania: US11.1
 */
@Serializable
data object ProgressRoute : NavKey
