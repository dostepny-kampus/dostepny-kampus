/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.campus

import io.github.dostepnykampus.core.model.building.BuildingCatalog
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dane mapy liczone raz na proces dla tych samych budynków i wejść: nowy model widoku po „Wstecz” i ten sam katalog
 * z innego źródła dostają ostatni wynik od razu. Pamięta jeden, ostatni katalog.
 *
 * Wymagania: NFR-11, US1
 *
 * @param compute liczenie danych mapy z katalogu; w aplikacji [campusMapData]
 */
@Singleton
class CampusMapDataCache internal constructor(private val compute: (BuildingCatalog) -> CampusMapData) {
    @Volatile
    private var last: Pair<Any, CampusMapData>? = null

    @Inject
    constructor() : this(::campusMapData)

    /** Dane mapy [catalog]: zapamiętane dla tych samych budynków i wejść, inaczej liczone od nowa. */
    fun dataFor(catalog: BuildingCatalog): CampusMapData {
        val key = catalog.buildings to catalog.entrances
        return last?.takeIf { it.first == key }?.second ?: compute(catalog).also { last = key to it }
    }
}
