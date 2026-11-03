/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.geo

/**
 * Nazwa budynku do pokazania obok symbolu (US1, decyzja 75).
 *
 * Nazwa złożona wyłącznie z symboli budynków, np. „C-13”, „C-3/C-4” albo „T-22” przy T-18, to brak nazwy:
 * ekran pokaże wtedy sam symbol. Ta sama reguła obowiązuje dla pliku konturów i dla odpowiedzi serwera.
 *
 * @param name nazwa z danych albo `null`
 * @return nazwa albo `null`, gdy dane nie mają nazwy innej niż symbole
 */
fun buildingName(name: String?): String? = name?.takeUnless { SYMBOLS_ONLY.matches(it.trim()) }

private val SYMBOLS_ONLY = Regex("""[A-Z]-\d+[a-z]?(\s*/\s*[A-Z]-\d+[a-z]?)*""")
