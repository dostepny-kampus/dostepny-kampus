/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model

/**
 * Budynki z konturami i ich wejścia z jednego zbioru danych, np. z konturów OpenStreetMap w aplikacji (US1, US17.1).
 *
 * @property buildings budynki, każdy symbol raz
 * @property entrances wejścia budynków z [buildings]
 * @property attribution atrybucja źródła danych do podpisu na mapie, np. „© OpenStreetMap contributors” (NFR-37)
 */
data class BuildingCatalog(val buildings: List<Building>, val entrances: List<Entrance>, val attribution: String)
