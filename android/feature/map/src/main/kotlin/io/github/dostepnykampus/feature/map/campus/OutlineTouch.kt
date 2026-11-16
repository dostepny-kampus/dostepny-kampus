/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.campus

/**
 * Symbole budynków dotkniętych jednym dotknięciem: kontury pod samym punktem [hit], a gdy punkt nie trafia
 * w żaden kontur, kontury w zasięgu dotknięcia [near]. Trafienie w kontur daje więc jego budynek, nawet obok
 * innego konturu; kontur wspólny daje swoje symbole.
 *
 * Wymagania: US2.8, NFR-17
 */
internal fun CampusMapData.touchedSymbols(hit: List<Int>, near: () -> List<Int>): List<String> =
    symbolsAt(hit).ifEmpty { symbolsAt(near()) }
