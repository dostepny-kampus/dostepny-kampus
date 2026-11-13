/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.buildings

import io.github.dostepnykampus.core.model.Entrance
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.BuildingDetails
import java.util.concurrent.ConcurrentHashMap

/** Wejścia budynku z serwera dla karty V2 i V3, z dniem i źródłem oznaczenia. Wymagania: US17.3 */
fun interface ServerEntrances {
    /**
     * Wejścia budynku [symbol] z serwera, każde z [Entrance.assessment]. Pusta lista, gdy serwer ich nie podał
     * (brak sieci, błąd serwera, nieznany budynek) i nic nie pobrano wcześniej; karta zostaje wtedy przy wejściach
     * z pliku konturów.
     *
     * Wymagania: NFR-10
     *
     * @param symbol symbol budynku
     */
    suspend fun entrances(symbol: String): List<Entrance>
}

/**
 * [ServerEntrances] z karty budynku z serwera (`GET /buildings/{symbol}`).
 *
 * Każde wywołanie pyta serwer przez [details]; zapytanie i odczyt odpowiedzi idą poza wątkiem UI w kliencie
 * HTTP. Wejścia z [ApiResult.Success] są zapamiętywane dla symbolu; przy innym wyniku (brak sieci, błąd serwera,
 * nieznany budynek) zostają wejścia pobrane wcześniej, a bez nich pusta lista. Pamięć żyje tyle co
 * obiekt, więc w aplikacji jest jeden na proces.
 *
 * Wymagania: NFR-10
 *
 * @param details karta budynku z serwera, np. `BuildingsApi::building`
 */
class ServerEntrancesRepository(private val details: suspend (String) -> ApiResult<BuildingDetails>) :
    ServerEntrances {
    private val downloaded = ConcurrentHashMap<String, List<Entrance>>()

    override suspend fun entrances(symbol: String): List<Entrance> {
        val result = details(symbol)
        return if (result is ApiResult.Success) {
            result.value.entrances.also { downloaded[symbol] = it }
        } else {
            downloaded[symbol].orEmpty()
        }
    }
}
