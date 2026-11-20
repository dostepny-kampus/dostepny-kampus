/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.camera

import io.github.dostepnykampus.feature.map.campus.BARRIER_ID
import io.github.dostepnykampus.feature.map.campus.ENTRANCE_ID
import io.github.dostepnykampus.feature.map.style.BARRIERS_LAYER
import io.github.dostepnykampus.feature.map.style.ENTRANCES_LAYER
import io.github.dostepnykampus.feature.map.style.PLAN_ROOM_ID
import io.github.dostepnykampus.feature.map.style.PLAN_SIGNS_LAYER

/**
 * Znak mapy V1, który reaguje na dotknięcie, z warstwą [layer] i właściwością [idKey] z identyfikatorem obiektu.
 * Kolejność wpisów to pierwszeństwo przy kilku znakach w zasięgu dotknięcia.
 *
 * Wymagania: US1.5, US2.1, US16.1, NFR-17
 */
enum class TouchSign(internal val layer: String, internal val idKey: String) {
    /** Znak bariery ze zgłoszenia przyjętego. */
    BARRIER(layer = BARRIERS_LAYER, idKey = BARRIER_ID),

    /** Znak wejścia do budynku. */
    ENTRANCE(layer = ENTRANCES_LAYER, idKey = ENTRANCE_ID),

    /** Znak windy, toalety dostosowanej albo schodów na planie piętra. */
    PLAN(layer = PLAN_SIGNS_LAYER, idKey = PLAN_ROOM_ID),
}
