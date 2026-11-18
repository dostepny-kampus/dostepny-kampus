/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.building

import io.github.dostepnykampus.core.data.server.ServerCard
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.building.EvacuationExit
import java.util.concurrent.ConcurrentHashMap

/** Wyjścia ewakuacyjne budynku z planów PWr z serwera dla karty V2. Wymagania: US2.6, BR-27 */
public fun interface ServerEvacuationExits {
    /**
     * Wyjścia ewakuacyjne budynku [symbol] z serwera. Pusta lista, gdy budynek ich nie ma albo serwer ich nie podał
     * (brak sieci, błąd serwera, nieznany budynek) i nic nie pobrano wcześniej.
     *
     * Wymagania: US2.6, NFR-10
     *
     * @param symbol symbol budynku
     */
    public suspend fun evacuationExits(symbol: String): List<EvacuationExit>
}

/**
 * [ServerEvacuationExits] z karty budynku z serwera (`GET /buildings/{symbol}`).
 *
 * Każde wywołanie pyta serwer przez [details]. Wyjścia z [ServerReply.Ok] są zapamiętywane dla symbolu; przy innym
 * wyniku zostają wyjścia pobrane wcześniej, a bez nich pusta lista. Pamięć żyje tyle co obiekt.
 *
 * Wymagania: US2.6, NFR-10
 *
 * @param details karta budynku z serwera dla symbolu
 */
public class ServerEvacuationExitsRepository(private val details: suspend (String) -> ServerReply<ServerCard>) :
    ServerEvacuationExits {
    private val downloaded = ConcurrentHashMap<String, List<EvacuationExit>>()

    override suspend fun evacuationExits(symbol: String): List<EvacuationExit> {
        val result = details(symbol)
        return if (result is ServerReply.Ok) {
            result.value.evacuationExits.also { downloaded[symbol] = it }
        } else {
            downloaded[symbol].orEmpty()
        }
    }
}
