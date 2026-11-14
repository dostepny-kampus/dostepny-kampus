/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.buildings

import io.github.dostepnykampus.core.data.server.ServerCard
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.Entrance
import java.util.concurrent.ConcurrentHashMap

/** Wejścia budynku z serwera dla karty V2 i V3, z dniem i źródłem oznaczenia. Wymagania: US17.3 */
public fun interface ServerEntrances {
    /**
     * Wejścia budynku [symbol] z serwera, każde z [Entrance.assessment]. Pusta lista, gdy serwer ich nie podał
     * (brak sieci, błąd serwera, nieznany budynek) i nic nie pobrano wcześniej; karta zostaje wtedy przy wejściach
     * z pliku konturów.
     *
     * Wymagania: NFR-10
     *
     * @param symbol symbol budynku
     */
    public suspend fun entrances(symbol: String): List<Entrance>
}

/**
 * [ServerEntrances] z karty budynku z serwera (`GET /buildings/{symbol}`).
 *
 * Każde wywołanie pyta serwer przez [details]; zapytanie i odczyt odpowiedzi idą poza wątkiem UI w kliencie
 * HTTP. Wejścia z [ServerReply.Ok] są zapamiętywane dla symbolu; przy innym wyniku (brak sieci, błąd serwera,
 * nieznany budynek) zostają wejścia pobrane wcześniej, a bez nich pusta lista. Pamięć żyje tyle co
 * obiekt, więc w aplikacji jest jeden na proces.
 *
 * Wymagania: NFR-10
 *
 * @param details karta budynku z serwera dla symbolu
 */
public class ServerEntrancesRepository(private val details: suspend (String) -> ServerReply<ServerCard>) : ServerEntrances {
    private val downloaded = ConcurrentHashMap<String, List<Entrance>>()

    override suspend fun entrances(symbol: String): List<Entrance> {
        val result = details(symbol)
        return if (result is ServerReply.Ok) {
            result.value.entrances.also { downloaded[symbol] = it }
        } else {
            downloaded[symbol].orEmpty()
        }
    }
}
