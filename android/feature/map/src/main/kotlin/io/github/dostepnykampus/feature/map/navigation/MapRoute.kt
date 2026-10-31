/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * V1 Mapa kampusu: ekran startowy, jedyny bez „Wstecz” (US1.2, wymagania wspólne). [focus] to budynek albo
 * wejście do zaznaczenia przy otwarciu, np. po „Pokaż na mapie” na V2 albo „Wstecz” z V2 (US1.1, US2.1).
 */
@Serializable
data class MapRoute(val focus: MapFocus? = null) : NavKey

/** Co V1 zaznacza na mapie. */
@Serializable
sealed interface MapFocus {
    /** Budynek o symbolu [symbol], np. `C-13`. */
    @Serializable
    data class Building(val symbol: String) : MapFocus

    /** Wejście o identyfikatorze [id] z danych wejść (BR-25). */
    @Serializable
    data class Entrance(val id: String) : MapFocus
}

/** V2 Karta budynku o symbolu [symbol] (US2, US3.2). */
@Serializable
data class BuildingRoute(val symbol: String) : NavKey

/** V3 Którędy wejść do budynku o symbolu [symbol] (US3). */
@Serializable
data class EntrancesRoute(val symbol: String) : NavKey
