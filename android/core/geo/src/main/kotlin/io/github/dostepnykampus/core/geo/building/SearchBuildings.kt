/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.geo.building

import io.github.dostepnykampus.core.model.building.Building
import java.text.Normalizer

/**
 * Podpowiedzi budynków po początku symbolu albo po słowie nazwy, bez sieci.
 *
 * Najpierw stoją budynki, których symbol zaczyna się od wpisanego tekstu. Wielkość liter nie ma znaczenia, a między
 * literą i numerem może stać dywiz, inna kreska, odstęp albo nic: „A” znajduje A-1, A-2 i A-10, „c1” znajduje C-1
 * i C-13, a „c13”, „C 13” i „C–13” znajdują C-13. Od trzeciego znaku dochodzą budynki, których nazwa ma słowo
 * zaczynające się od wpisanego tekstu, bez wielkości liter i polskich znaków: „glow” znajduje „Budynek główny”.
 * Tekst, który nie pasuje ani do symbolu, ani do nazwy, np. „A-1 329”, albo pusty daje pusty wynik.
 *
 * Wymagania: US1.1
 *
 * @param query tekst wpisany w pole wyszukiwania
 * @param buildings budynki, wśród których szukamy
 * @return budynki dopasowane po symbolu, a za nimi po nazwie, każda grupa w kolejności [buildings]; pusta lista,
 *   gdy brak wyniku
 */
public fun searchBuildings(query: String, buildings: List<Building>): List<Building> {
    val bySymbol =
        symbolStart(query)
            ?.let { start -> buildings.filter { it.symbol.hasSymbolStart(start) } }
            .orEmpty()
    val byName =
        nameStart(query)
            ?.let { start -> buildings.filter { it !in bySymbol && it.name.hasWordStart(start) } }
            .orEmpty()
    return bySymbol + byName
}

/**
 * Budynek z [buildings], którego symbol stoi w [query] przed końcowym numerem sali, np. A-1 dla „A-1 999”.
 * Symbol musi być taki sam jak w zapytaniu po normalizacji zapisu; bez numeru na końcu wynik to `null`.
 *
 * @param query tekst wpisany w pole wyszukiwania
 * @param buildings budynki, wśród których szukamy symbolu
 */
public fun buildingBeforeRoom(query: String, buildings: List<Building>): Building? {
    val parts = query.trim().split(' ').filter(String::isNotBlank)
    val hasRoomNumber = parts.size >= 2 && parts.last().first().isDigit()
    val start = if (hasRoomNumber) symbolStart(parts.dropLast(1).joinToString(separator = " ")) else null
    return start?.let { symbol -> buildings.firstOrNull { symbolStart(it.symbol) == symbol } }
}

private const val NAME_MIN_LENGTH = 3

private val SYMBOL_START = Regex("""([A-Za-z])\s*[-‐-―−]?\s*(\d*)([A-Za-z]?)""")

private val MARKS = Regex("""\p{M}+""")

private fun symbolStart(text: String): String? =
    SYMBOL_START
        .matchEntire(text.trim())
        ?.destructured
        ?.takeIf { (_, number, suffix) -> number.isNotEmpty() || suffix.isEmpty() }
        ?.let { (letter, number, suffix) -> "${letter.uppercase()}-$number${suffix.lowercase()}" }

private fun String.hasSymbolStart(start: String): Boolean = symbolStart(this)?.startsWith(start) == true

private fun nameStart(text: String): String? = folded(text.trim()).takeIf { it.length >= NAME_MIN_LENGTH }

private fun String?.hasWordStart(start: String): Boolean =
    this != null && Regex("""(^|[^\p{L}\p{N}])${Regex.escape(start)}""").containsMatchIn(folded(this))

private fun folded(text: String): String =
    MARKS
        .replace(input = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD), replacement = "")
        .replace(oldChar = 'ł', newChar = 'l')
