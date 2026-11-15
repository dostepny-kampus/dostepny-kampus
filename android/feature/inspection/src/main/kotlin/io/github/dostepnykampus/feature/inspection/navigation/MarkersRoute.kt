/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * C5 „Obejrzane piętra” w wariancie Koordynatora, bez paska nawigacji; wejście z K1 „Otwórz”.
 *
 * Wymagania: US11.3
 *
 * @property buildingSymbol symbol budynku, którego znaczniki pokazuje ekran
 */
@Serializable
data class MarkersRoute(val buildingSymbol: String) : NavKey
