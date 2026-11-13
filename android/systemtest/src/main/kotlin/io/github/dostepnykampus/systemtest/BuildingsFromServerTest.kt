/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.systemtest

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.dostepnykampus.core.network.AccessTokens
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.NetworkModule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Aplikacja na emulatorze i serwer z bazą na komputerze gospodarza: klient i parser kontraktu z aplikacji czytają
 * listę budynków i kartę budynku z prawdziwej odpowiedzi serwera. Pole odpowiedzi niezgodne z aplikacją daje
 * wynik inny niż [ApiResult.Success].
 */
@RunWith(AndroidJUnit4::class)
class BuildingsFromServerTest {
    private val api = NetworkModule.buildingsApi(NetworkModule.okHttp(AccessTokens()), NetworkModule.json())

    /** Lista budynków z serwera nie jest pusta, a karta pierwszego budynku z listy daje się odczytać. */
    @Test
    fun listAndCardFromServer() {
        runBlocking {
            val list = api.listBuildings()
            assertTrue("Lista budynków: $list", list is ApiResult.Success && list.value.buildings.isNotEmpty())
            val buildings = (list as ApiResult.Success).value.buildings
            val symbol = buildings.first().building.symbol
            val card = api.building(symbol)
            assertTrue("Karta budynku $symbol: $card", card is ApiResult.Success)
        }
    }
}
