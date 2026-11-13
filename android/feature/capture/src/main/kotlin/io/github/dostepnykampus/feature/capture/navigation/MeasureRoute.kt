/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** C1 Pomiar pozycji, bez paska nawigacji. Wejście doda ekran pracy Zbierającego. Wymagania: US6.1 */
@Serializable
data object MeasureRoute : NavKey
