/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.compose.runtime.Immutable
import io.github.dostepnykampus.core.model.building.Building
import io.github.dostepnykampus.core.model.room.FoundRoom

/**
 * Podpowiedzi budynków po początku symbolu, a gdy żaden budynek nie pasuje, sale z serwera.
 *
 * Wymagania: US1.3, US18.1
 *
 * @property query zapytanie, dla którego policzono wynik; ekran pokazuje wynik tylko dla zapytania z pola
 * @property buildings budynki o symbolu zaczynającym się od [query] w kolejności katalogu; `null`, dopóki katalog
 *   się wczytuje
 * @property rooms sale z serwera, gdy żaden budynek nie pasuje, a tekst ma cyfry; pusta bez sieci i bez wyniku
 */
@Immutable
data class BuildingSearch(
    val query: String = "",
    val buildings: List<Building>? = null,
    val rooms: List<FoundRoom> = emptyList(),
)

/** Wynik dla zapytania [shown] z pola; `null`, gdy wynik jest dla innego zapytania albo katalog się wczytuje. */
fun BuildingSearch.foundFor(shown: String): List<Building>? = buildings.takeIf { query == shown }
