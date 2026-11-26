/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * C5 „Obejrzane piętra” w wariancie Zbierającego, bez paska nawigacji; wejście z karty budynku V2.
 *
 * Wymagania: US15.1
 *
 * @property buildingSymbol symbol budynku, którego piętra pokazuje ekran
 */
@Serializable
data class CollectorMarkersRoute(val buildingSymbol: String) : NavKey
