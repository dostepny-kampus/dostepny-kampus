/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.room

import io.github.dostepnykampus.feature.map.R

/** Szczegóły dostępne z kroku karty sali. Wymagania: US18.2 */
internal enum class RoomStepDetail(val title: Int) {
    ENTRANCE(R.string.mapa_sala_szczegoly_wejscia),
    LIFT(R.string.mapa_sala_szczegoly_windy),
}
