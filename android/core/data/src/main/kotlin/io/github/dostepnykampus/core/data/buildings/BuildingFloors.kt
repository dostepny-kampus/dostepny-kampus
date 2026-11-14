/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.buildings

import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.FloorList
import java.util.concurrent.ConcurrentHashMap

/** Piętra budynku z serwera dla formularza C2. Wymagania: US7.2, US16.1 */
fun interface BuildingFloors {
    /**
     * Piętra budynku [symbol] od najniższego; pusta lista dla budynku podstawowego. `null`, gdy serwer ich
     * nie podał (brak sieci, błąd serwera, nieznany budynek) i nic nie pobrano wcześniej; formularz zostaje wtedy przy
     * wpisaniu piętra, bo brak planu nie blokuje zgłoszenia.
     *
     * Wymagania: BR-G1
     *
     * @param symbol symbol budynku
     */
    suspend fun floors(symbol: String): FloorList?
}

/**
 * [BuildingFloors] z serwera (`GET /buildings/{symbol}/floors`). Piętra z [ServerReply.Ok] są zapamiętywane dla
 * symbolu; przy innym wyniku zostają piętra pobrane wcześniej. Pamięć żyje tyle co obiekt, w aplikacji jeden na proces.
 *
 * @param fetch piętra budynku z serwera dla symbolu
 */
class ServerFloorsRepository(private val fetch: suspend (String) -> ServerReply<FloorList>) : BuildingFloors {
    private val downloaded = ConcurrentHashMap<String, FloorList>()

    override suspend fun floors(symbol: String): FloorList? {
        val result = fetch(symbol)
        return if (result is ServerReply.Ok) result.value.also { downloaded[symbol] = it } else downloaded[symbol]
    }
}
