/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.geo

/**
 * Nazwa budynku do pokazania obok symbolu (US1, decyzja 75).
 *
 * Nazwa złożona wyłącznie z symboli budynków, np. „C-13”, „C-3/C-4” albo „T-22” przy T-18, to brak nazwy:
 * ekran pokaże wtedy sam symbol. Symbol na początku nazwy znika, gdy po nim, po „/” albo „-”, stoi dalsza nazwa
 * od wielkiej litery albo w cudzysłowie: „A-1 / Budynek główny” → „Budynek główny”, „T-16 "Tower"” → „"Tower"”.
 * Nazwa z symbolem w środku („Dom Studencki T-6 "Alcatraz"”) i dopisek „M-6 bis” zostają bez zmian, bo bez
 * symbolu straciłyby sens. Dotyczy tylko dołączonego pliku konturów; serwer sam pomija nazwę budynku bez nazwy.
 *
 * @param name nazwa z danych albo `null`
 * @return nazwa albo `null`, gdy dane nie mają nazwy innej niż symbole
 */
fun buildingName(name: String?): String? = name?.let(::withoutOwnSymbol)

private fun withoutOwnSymbol(name: String): String? {
    val trimmed = name.trim()
    if (SYMBOLS_ONLY.matches(trimmed)) return null
    return LEADING_SYMBOL.matchEntire(trimmed)?.let { it.groupValues[1] } ?: trimmed
}

private val SYMBOLS_ONLY = Regex("""[A-Z]-\d+[a-z]?(\s*/\s*[A-Z]-\d+[a-z]?)*""")

private val LEADING_SYMBOL = Regex("""[A-Z]-\d+[a-z]?\s*(?:[/-]\s*)?(["„\p{Lu}].*)""")
