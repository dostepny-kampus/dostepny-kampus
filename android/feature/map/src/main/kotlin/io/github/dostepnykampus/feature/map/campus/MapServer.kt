/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.campus

import io.github.dostepnykampus.core.data.barrier.AcceptedBarriers
import io.github.dostepnykampus.core.data.room.RoomSearch
import io.github.dostepnykampus.core.data.server.ServerReply
import javax.inject.Inject

/**
 * Dane mapy V1 z serwera: bariery ze zgłoszeń przyjętych [barriers] i wyszukiwanie sal [rooms].
 *
 * Wymagania: US1.3, US18.1
 */
class MapServer
    @Inject
    constructor(
        val barriers: AcceptedBarriers,
        val rooms: RoomSearch,
    )

/** Serwer bez barier i bez sal, np. w testach i podglądach. */
internal val NO_SERVER = MapServer(barriers = { emptyList() }, rooms = { ServerReply.Ok(emptyList()) })

/** Ten sam serwer z barierami [barriers] albo wyszukiwaniem sal [rooms] na miejscu dotychczasowych. */
internal fun MapServer.with(barriers: AcceptedBarriers = this.barriers, rooms: RoomSearch = this.rooms): MapServer =
    MapServer(barriers = barriers, rooms = rooms)
