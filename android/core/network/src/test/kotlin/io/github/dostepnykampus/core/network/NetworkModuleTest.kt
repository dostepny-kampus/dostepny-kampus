/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.network.auth.AccessTokenInterceptor
import io.github.dostepnykampus.core.network.auth.AccessTokens
import io.github.dostepnykampus.core.network.building.ApiBuilding
import io.github.dostepnykampus.core.network.building.BuildingsApi
import io.github.dostepnykampus.core.network.building.decodeBuildingList
import io.github.dostepnykampus.core.network.di.NetworkModule
import io.github.dostepnykampus.core.network.inspection.InspectionProgressApi
import io.github.dostepnykampus.core.network.inspection.MarkerChangesApi
import io.github.dostepnykampus.core.network.review.ReviewApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

/** Moduł Hilt składa klienta HTTP, parser i [BuildingsApi] bez uruchamiania serwera. */
class NetworkModuleTest {
    @Test
    fun buildsClientFromModule() {
        val http = NetworkModule.okHttp(AccessTokens())
        val json = NetworkModule.json()
        val api = NetworkModule.buildingsApi(http = http, json = json)
        assertEquals(CALL_MS, http.connectTimeoutMillis, "limit połączenia klienta")
        assertEquals(CALL_MS, http.callTimeoutMillis, "limit wywołania klienta")
        assertInstanceOf(BuildingsApi::class.java, api, "klient budynków z modułu")
        assertInstanceOf(
            MarkerChangesApi::class.java,
            NetworkModule.markerChangesApi(http = http, json = json),
            "klient zmian znaczników z modułu",
        )
        assertInstanceOf(
            InspectionProgressApi::class.java,
            NetworkModule.inspectionProgressApi(http = http, json = json),
            "klient postępu z modułu",
        )
        assertInstanceOf(
            ReviewApi::class.java,
            NetworkModule.reviewApi(http = http, json = json),
            "klient sprawdzania z modułu",
        )
        assertInstanceOf(AccessTokenInterceptor::class.java, http.interceptors.single(), "przechwytywacz tokenu konta")
        val parsed = json.decodeBuildingList("""{"source":"x","extra":1,"buildings":[]}""")
        assertEquals("x", parsed.source, "źródło odczytane mimo nieznanego pola")
        assertEquals(emptyList<ApiBuilding>(), parsed.buildings, "pusta lista budynków")
    }

    private companion object {
        const val CALL_MS = 3_000
    }
}
