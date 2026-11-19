/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.room

import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.data.server.toReply
import io.github.dostepnykampus.core.model.room.FoundRoom
import io.github.dostepnykampus.core.network.building.BuildingsApi

/**
 * Wyszukiwanie sali na serwerze. Wymaga sieci: bez niej wynik to [ServerReply.Offline], a nie pusta lista, żeby ekran
 * odróżnił brak sali od braku połączenia.
 *
 * Wymagania: US18.1
 */
public fun interface RoomSearch {
    /**
     * Sale pasujące do tekstu [query], np. „A-1 329”; najpierw dokładne dopasowania numeru.
     *
     * @param query tekst wpisany w pole wyszukiwania
     */
    public suspend fun search(query: String): ServerReply<List<FoundRoom>>
}

/**
 * Wyszukiwanie sali z klienta [api].
 *
 * @param api klient HTTP budynków i sal
 */
internal fun roomSearchOf(api: BuildingsApi): RoomSearch = RoomSearch { query -> api.searchRooms(query).toReply { it } }
