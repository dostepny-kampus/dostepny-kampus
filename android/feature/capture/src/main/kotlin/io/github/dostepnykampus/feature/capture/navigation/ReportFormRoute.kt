/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * C2 Opisz barierę (US7) z miejscem z pomiaru C1 (US6.1, US6.2). Trasa niesie tylko dane pomiaru, więc zapisana
 * historia „Wstecz” przetrwa zamknięcie aplikacji przez system.
 *
 * @property longitude długość geograficzna uśrednionego punktu
 * @property latitude szerokość geograficzna uśrednionego punktu
 * @property accuracyMeters dokładność pomiaru w metrach (NFR-2)
 * @property buildingSymbol budynek potwierdzony albo wybrany na C1; `null` to „poza budynkiem” (BR-G4)
 * @property buildingName nazwa budynku obok symbolu albo `null`, gdy dane jej nie mają
 */
@Serializable
data class ReportFormRoute(
    val longitude: Double,
    val latitude: Double,
    val accuracyMeters: Double,
    val buildingSymbol: String?,
    val buildingName: String? = null,
) : NavKey
