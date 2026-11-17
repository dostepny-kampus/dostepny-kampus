/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model.building

/**
 * Budynki z konturami i ich wejścia z jednego zbioru danych, np. z konturów OpenStreetMap w aplikacji.
 *
 * Wymagania: US1, US17.1, NFR-37, BR-G1, US16.1
 *
 * @property buildings budynki, każdy symbol raz
 * @property entrances wejścia budynków z [buildings]
 * @property attribution atrybucja źródła danych do podpisu na mapie, np. „© OpenStreetMap contributors”
 * @property detailed symbole budynków szczegółowych, które mają plan pięter PWr; zna je tylko lista serwera, więc
 *   katalog z pliku konturów ma zbiór pusty
 * @property hasServerList czy katalog powstał z listy serwera; bez niej [detailed] jest pusty, bo nie wiadomo,
 *   które budynki mają plan
 */
public data class BuildingCatalog(
    val buildings: List<Building>,
    val entrances: List<Entrance>,
    val attribution: String,
    val detailed: Set<String> = emptySet(),
    val hasServerList: Boolean = false,
)
