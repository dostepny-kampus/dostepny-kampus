/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.buildings

import io.github.dostepnykampus.core.model.Entrance
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.BuildingDetails

/** Wejścia budynku z serwera dla karty V2 i V3, z dniem i źródłem oznaczenia (US17.3). */
fun interface ServerEntrances {
    /**
     * Wejścia budynku [symbol] z serwera, każde z [Entrance.assessment]. Pusta lista, gdy serwer ich nie podał
     * (brak sieci, błąd serwera, nieznany budynek) i nic nie pobrano wcześniej; karta zostaje wtedy przy wejściach
     * z pliku konturów (NFR-10).
     *
     * @param symbol symbol budynku
     */
    suspend fun entrances(symbol: String): List<Entrance>
}

/**
 * [ServerEntrances] z karty budynku z serwera (`GET /buildings/{symbol}`).
 *
 * Na razie karta nie pyta serwera: wynik jest zawsze pusty, a wejścia karty są z pliku konturów.
 *
 * @param details karta budynku z serwera, np. `BuildingsApi::building`
 */
class ServerEntrancesRepository(details: suspend (String) -> ApiResult<BuildingDetails>) : ServerEntrances {
    override suspend fun entrances(symbol: String): List<Entrance> = emptyList()
}
