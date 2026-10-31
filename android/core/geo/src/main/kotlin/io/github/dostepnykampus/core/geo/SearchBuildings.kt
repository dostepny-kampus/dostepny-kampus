/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.geo

import io.github.dostepnykampus.core.model.Building

/**
 * Wyszukanie budynku po symbolu w różnym zapisie (US1.1), bez sieci.
 *
 * Wielkość liter nie ma znaczenia, a między literą i numerem może stać dywiz, inna kreska, odstęp albo nic,
 * np. „c13”, „c-13”, „C 13” i „C–13” znajdują C-13. Symbol musi się zgadzać w całości: „C1” znajduje C-1,
 * a nie C-13.
 * Tekst, który nie jest symbolem budynku, np. „A-1 329”, albo symbol spoza [buildings] daje pusty wynik.
 *
 * @param query tekst wpisany w pole symbolu budynku
 * @param buildings budynki, wśród których szukamy
 * @return budynki o tym symbolu w kolejności [buildings]; pusta lista, gdy brak wyniku
 */
fun searchBuildings(query: String, buildings: List<Building>): List<Building> {
    val key = symbolKey(query) ?: return emptyList()
    return buildings.filter { symbolKey(it.symbol) == key }
}

private val SYMBOL = Regex("""([A-Za-z])\s*[-‐-―−]?\s*(\d+)([A-Za-z]?)""")

private fun symbolKey(text: String): String? =
    SYMBOL.matchEntire(text.trim())?.let { match ->
        val (letter, number, suffix) = match.destructured
        "${letter.uppercase()}-$number${suffix.lowercase()}"
    }
