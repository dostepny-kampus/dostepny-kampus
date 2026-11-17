/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.geo.building

import io.github.dostepnykampus.core.model.building.Building

/**
 * Podpowiedzi budynków po początku symbolu w różnym zapisie, bez sieci.
 *
 * Wynik to budynki, których symbol zaczyna się od wpisanego tekstu. Wielkość liter nie ma znaczenia, a między
 * literą i numerem może stać dywiz, inna kreska, odstęp albo nic: „A” znajduje A-1, A-2 i A-10, „c1” znajduje C-1
 * i C-13, a „c13”, „C 13” i „C–13” znajdują C-13. Tekst, który nie jest początkiem symbolu, np. „A-1 329”, albo
 * pusty daje pusty wynik.
 *
 * Wymagania: US1.1
 *
 * @param query tekst wpisany w pole wyszukiwania
 * @param buildings budynki, wśród których szukamy
 * @return budynki o symbolu zaczynającym się od [query] w kolejności [buildings]; pusta lista, gdy brak wyniku
 */
public fun searchBuildings(query: String, buildings: List<Building>): List<Building> {
    val start = symbolStart(query) ?: return emptyList()
    return buildings.filter { building -> symbolStart(building.symbol)?.startsWith(start) == true }
}

private val SYMBOL_START = Regex("""([A-Za-z])\s*[-‐-―−]?\s*(\d*)([A-Za-z]?)""")

private fun symbolStart(text: String): String? =
    SYMBOL_START
        .matchEntire(text.trim())
        ?.destructured
        ?.takeIf { (_, number, suffix) -> number.isNotEmpty() || suffix.isEmpty() }
        ?.let { (letter, number, suffix) -> "${letter.uppercase()}-$number${suffix.lowercase()}" }
