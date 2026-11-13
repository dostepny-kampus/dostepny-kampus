/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.geo

import io.github.dostepnykampus.core.model.Building

/** Podpowiedź budynku dla punktu, odpowiednik `LocateResult` z kontraktu API. Wymagania: US6.2, BR-G2 */
public sealed interface LocateResult {
    /**
     * Punkt leży w konturze budynku.
     *
     * @property building budynek, w którego konturze leży punkt
     */
    public data class InsideOutline(val building: Building) : LocateResult

    /**
     * Stan „Poza budynkiem”: punkt nie leży w konturze żadnego budynku. To stan normalny, nie błąd.
     *
     * Wymagania: BR-G2
     *
     * @property nearest najbliższy budynek do potwierdzenia albo odrzucenia; `null`, gdy lista budynków jest pusta
     */
    public data class OutsideBuilding(val nearest: Building?) : LocateResult
}
