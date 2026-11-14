/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.compose.runtime.Immutable
import io.github.dostepnykampus.core.model.Building

/**
 * Wynik wyszukiwania budynku po symbolu.
 *
 * Wymagania: US1.3
 *
 * @property query zapytanie, dla którego policzono wynik; ekran pokazuje wynik tylko dla zapytania z pola
 * @property buildings budynki o tym symbolu w kolejności katalogu; `null`, dopóki katalog się wczytuje
 */
@Immutable
data class BuildingSearch(val query: String = "", val buildings: List<Building>? = null)

/** Wynik dla zapytania [shown] z pola; `null`, gdy wynik jest dla innego zapytania albo katalog się wczytuje. */
fun BuildingSearch.foundFor(shown: String): List<Building>? = buildings.takeIf { query == shown }
