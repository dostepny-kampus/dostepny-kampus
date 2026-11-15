/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.barrier

import androidx.compose.runtime.Immutable
import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.model.building.Building

/**
 * Bariera wybrana na mapie razem z tym, czego arkusz bariery nie ma w samym zgłoszeniu.
 *
 * Wymagania: US1.5
 *
 * @property barrier bariera ze zgłoszenia przyjętego
 * @property building budynek bariery z katalogu; `null` to bariera poza budynkiem albo budynek spoza katalogu
 * @property entrance nazwa wejścia z danych; `null`, gdy zgłoszenie go nie wskazuje albo dane nie mają nazwy
 */
@Immutable
internal data class ChosenBarrier(val barrier: AcceptedBarrier, val building: Building?, val entrance: String?)
