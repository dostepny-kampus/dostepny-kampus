/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.campus

import io.github.dostepnykampus.core.data.barrier.AcceptedBarriers
import io.github.dostepnykampus.core.data.path.AccessiblePaths
import io.github.dostepnykampus.core.data.room.RoomSearch
import io.github.dostepnykampus.core.data.server.ServerReply
import javax.inject.Inject

/**
 * Dane mapy V1 z serwera: bariery ze zgłoszeń przyjętych [barriers], wyszukiwanie sal [rooms] i ścieżki
 * dostosowane [paths].
 *
 * Wymagania: US1.3, US18.1, US1.4
 */
data class MapServer
    @Inject
    constructor(
        val barriers: AcceptedBarriers,
        val rooms: RoomSearch,
        val paths: AccessiblePaths,
    )

/** Serwer bez barier, sal i ścieżek, np. w testach i podglądach. */
internal val NO_SERVER =
    MapServer(barriers = { emptyList() }, rooms = { ServerReply.Ok(emptyList()) }, paths = { emptyList() })
