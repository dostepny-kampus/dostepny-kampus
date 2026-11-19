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
 * Podpowiedzi budynków po początku symbolu, a gdy żaden budynek nie pasuje, sale z serwera albo prośba o budynek.
 *
 * Wymagania: US1.3, US18.1
 *
 * @property query zapytanie, dla którego policzono wynik; ekran pokazuje wynik tylko dla zapytania z pola
 * @property buildings budynki o symbolu zaczynającym się od [query] w kolejności katalogu; `null`, dopóki katalog
 *   się wczytuje
 * @property rooms sale z serwera, gdy żaden budynek nie pasuje, a tekst ma cyfry i symbol budynku; pusta bez sieci
 *   i bez wyniku
 * @property isBuildingMissing czy [query] to sam numer sali bez symbolu budynku; sali bez budynku model nie szuka
 */
@Immutable
data class BuildingSearch(
    val query: String = "",
    val buildings: List<Building>? = null,
    val rooms: List<FoundRoom> = emptyList(),
    val isBuildingMissing: Boolean = false,
)

/** Wynik dla zapytania [shown] z pola; `null`, gdy wynik jest dla innego zapytania albo katalog się wczytuje. */
fun BuildingSearch.foundFor(shown: String): List<Building>? = buildings.takeIf { query == shown }

private val RoomQueryNoise = Regex(pattern = """(?iu)(?<![\p{L}\d])(?:sala|pok\.|s\.)(?!\p{L})|[,/]""")

/**
 * Czy [query] to sam numer sali, np. „329” albo „sala 329”: jedna część zaczynająca się cyfrą, bez symbolu budynku.
 *
 * Wymagania: US18.1
 */
fun isRoomNumberOnly(query: String): Boolean {
    val parts =
        RoomQueryNoise
            .replace(input = query, replacement = " ")
            .split(' ')
            .filter(String::isNotBlank)
    return parts.size == 1 && parts.single().first().isDigit()
}
