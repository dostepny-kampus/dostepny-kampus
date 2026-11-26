/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * C6 „Zmiana oznaczenia wejścia” dla wejścia [entranceId] budynku [buildingSymbol]; z [submissionId] poprawka własnej
 * zmiany z kolejki telefonu, bez niego nowa zmiana.
 *
 * Wymagania: US17.2, US8.3
 */
@Serializable
data class MarkingRoute(val entranceId: String, val buildingSymbol: String, val submissionId: String? = null) : NavKey
