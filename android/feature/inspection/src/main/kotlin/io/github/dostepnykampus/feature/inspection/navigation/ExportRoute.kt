/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** K3 Eksport, bez paska nawigacji; wejście z L2 tylko dla Koordynatora. Wymagania: US13 */
@Serializable
data object ExportRoute : NavKey
