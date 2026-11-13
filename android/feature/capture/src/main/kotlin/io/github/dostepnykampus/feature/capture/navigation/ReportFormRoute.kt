/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * C2 Opisz barierę z miejscem z pomiaru C1. Trasa niesie tylko dane pomiaru, więc zapisana
 * historia „Wstecz” przetrwa zamknięcie aplikacji przez system.
 *
 * Wymagania: US7, US6.1, US6.2, NFR-2, BR-G4
 *
 * @property longitude długość geograficzna uśrednionego punktu
 * @property latitude szerokość geograficzna uśrednionego punktu
 * @property accuracyMeters dokładność pomiaru w metrach
 * @property buildingSymbol budynek potwierdzony albo wybrany na C1; `null` to „poza budynkiem”
 * @property buildingName nazwa budynku obok symbolu albo `null`, gdy dane jej nie mają
 * @property isManualCorrection Zbierający przesunął punkt na mapie po pomiarze (US6.4, NFR-3)
 */
@Serializable
data class ReportFormRoute(
    val longitude: Double,
    val latitude: Double,
    val accuracyMeters: Double,
    val buildingSymbol: String?,
    val buildingName: String? = null,
    val isManualCorrection: Boolean = false,
) : NavKey
