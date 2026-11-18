/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.building

import io.github.dostepnykampus.core.data.server.ServerCard
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EvacuationExit
import java.util.concurrent.ConcurrentHashMap

/**
 * Wejścia i wyjścia ewakuacyjne budynku z serwera dla karty V2, wejścia z dniem i źródłem oznaczenia.
 *
 * Wymagania: US17.3, US2.6
 */
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

    /**
     * Wejścia jak w [entrances] i wyjścia ewakuacyjne budynku [symbol] z planów PWr z jednej odpowiedzi serwera.
     * Wyjść nie ma, gdy budynek ich nie ma albo serwer ich nie podał i nic nie pobrano wcześniej; bez nadpisania
     * same wejścia z [entrances].
     * TODO(#3165): karta V2 jeszcze nie pyta o wyjścia ewakuacyjne.
     *
     * Wymagania: US2.6, BR-27, NFR-10
     *
     * @param symbol symbol budynku
     */
    public suspend fun cardEntrances(symbol: String): CardEntrances = CardEntrances(entrances(symbol))
}

/**
 * Wejścia i wyjścia ewakuacyjne budynku z tej samej karty budynku z serwera.
 *
 * Wymagania: US2.6, US17.3
 *
 * @property entrances wejścia budynku
 * @property evacuationExits wyjścia ewakuacyjne budynku; pusta lista, gdy ich nie ma
 */
public data class CardEntrances(
    val entrances: List<Entrance>,
    val evacuationExits: List<EvacuationExit> = emptyList(),
)

/**
 * [ServerEntrances] z karty budynku z serwera (`GET /buildings/{symbol}`).
 *
 * Każde wywołanie pyta serwer przez [details] raz; zapytanie i odczyt odpowiedzi idą poza wątkiem UI w kliencie
 * HTTP. Karta z [ServerReply.Ok] jest zapamiętywana dla symbolu; przy innym wyniku (brak sieci, błąd serwera,
 * nieznany budynek) zostają wejścia i wyjścia pobrane wcześniej, a bez nich puste listy. Pamięć żyje tyle co
 * obiekt, więc w aplikacji jest jeden na proces.
 *
 * Wymagania: NFR-10
 *
 * @param details karta budynku z serwera dla symbolu
 */
public class ServerEntrancesRepository(private val details: suspend (String) -> ServerReply<ServerCard>) :
    ServerEntrances {
    private val downloaded = ConcurrentHashMap<String, ServerCard>()

    override suspend fun entrances(symbol: String): List<Entrance> = cardEntrances(symbol).entrances

    override suspend fun cardEntrances(symbol: String): CardEntrances {
        val result = details(symbol)
        val card = if (result is ServerReply.Ok) result.value.also { downloaded[symbol] = it } else downloaded[symbol]
        return CardEntrances(card?.entrances.orEmpty(), card?.evacuationExits.orEmpty())
    }
}
