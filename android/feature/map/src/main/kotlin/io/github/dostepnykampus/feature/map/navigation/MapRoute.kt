/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * V1 Mapa kampusu: ekran startowy, jedyny bez „Wstecz” (wymagania wspólne). [focus] to budynek albo
 * wejście do zaznaczenia przy otwarciu, np. po „Pokaż na mapie” na V2 albo „Wstecz” z V2.
 *
 * Wymagania: US1.2, US1.1, US2.1
 */
@Serializable
data class MapRoute(val focus: MapFocus? = null) : NavKey

/** Co V1 zaznacza na mapie. */
@Serializable
sealed interface MapFocus {
    /**
     * Budynek o symbolu [symbol], np. `C-13`. [command] to tożsamość polecenia „Pokaż budynek na mapie”, po którym
     * kamera staje na budynku; każde dotknięcie przycisku dostaje inną. Bez niego (`null`) budynek jest tylko
     * zaznaczony, a widok zostaje, np. po „Wstecz” z V2. [floor] to piętro planu do pokazania po „Pokaż na planie”;
     * budynek z piętrem nie jest zaznaczony. `null` zostawia piętro wybrane przez plan i zaznaczenie.
     *
     * Wymagania: US2.9, US16.1
     */
    @Serializable
    data class Building(val symbol: String, val command: Long? = null, val floor: String? = null) : MapFocus

    /**
     * Wejście o identyfikatorze [id] z danych wejść, pokazane z ekranu budynku [buildingSymbol]; TalkBack
     * czyta ten budynek także dla wejścia wspólnego kilku budynków. [isFullSheet]: ekran budynku był od razu na
     * cały ekran, więc arkusz tego wejścia też otwiera się na cały ekran.
     *
     * Wymagania: BR-25, NFR-19
     */
    @Serializable
    data class Entrance(val id: String, val buildingSymbol: String, val isFullSheet: Boolean = false) : MapFocus
}

/**
 * V2 Karta budynku o symbolu [symbol]. [isDirect]: karta otwarta od razu na pełnym ekranie, bez
 * małej karty, bo budynek wybrano z listy przy TalkBack.
 *
 * Wymagania: US2, US3.2, NFR-19
 */
@Serializable
data class BuildingRoute(val symbol: String, val isDirect: Boolean = false) : NavKey
