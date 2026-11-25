/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** C6 „Zmiana oznaczenia wejścia” dla wejścia [entranceId] budynku [buildingSymbol]. Wymagania: US17.2 */
@Serializable
data class MarkingRoute(val entranceId: String, val buildingSymbol: String) : NavKey
