/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.campus

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Floor
import io.github.dostepnykampus.core.model.room.RoomList

/**
 * Plan piętra pokazany na mapie.
 *
 * Wymagania: US16.1
 *
 * @property building symbol budynku z planem
 * @property floors piętra budynku od najniższego
 * @property floor identyfikator wybranego piętra
 * @property rooms pomieszczenia pięter z kopii planu, według identyfikatora piętra
 * @property room identyfikator wyróżnionej sali tego budynku; `null` bez wyróżnienia
 */
data class ShownPlan(
    val building: String,
    val floors: List<Floor>,
    val floor: String,
    val rooms: Map<String, RoomList>,
    val room: String? = null,
)

/**
 * Znacznik kroku [index] na planie w punkcie [point]. [selected] oznacza aktywny krok, a [isDestination]
 * ostatni krok przy sali, którego znak odsuwa się od numeru pomieszczenia. Wymagania: US18.2.
 */
data class RoomStepMarker(val index: Int, val point: GeoPoint, val selected: Boolean, val isDestination: Boolean)
