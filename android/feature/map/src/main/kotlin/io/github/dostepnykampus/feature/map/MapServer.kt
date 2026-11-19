/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.core.data.barrier.AcceptedBarriers
import io.github.dostepnykampus.core.data.room.RoomSearch
import io.github.dostepnykampus.core.data.server.ServerReply
import javax.inject.Inject

/**
 * Dane mapy V1 z serwera: bariery ze zgłoszeń przyjętych [barriers] i wyszukiwanie sal [rooms]. Bez serwera, np.
 * w testach, nie ma barier ani sal.
 *
 * Wymagania: US1.3, US18.1
 */
class MapServer
    @Inject
    constructor(
        val barriers: AcceptedBarriers = AcceptedBarriers { emptyList() },
        val rooms: RoomSearch = RoomSearch { ServerReply.Ok(emptyList()) },
    )
