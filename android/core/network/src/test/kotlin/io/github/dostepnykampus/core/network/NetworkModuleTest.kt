/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

/** Moduł Hilt składa klienta HTTP, parser i [BuildingsApi] bez uruchamiania serwera. */
class NetworkModuleTest {
    @Test
    fun client_from_module() {
        val http = NetworkModule.okHttp()
        val json = NetworkModule.json()
        val api = NetworkModule.buildingsApi(http = http, json = json)
        assertEquals(CALL_MS, http.connectTimeoutMillis)
        assertEquals(CALL_MS, http.callTimeoutMillis)
        assertEquals(BASE_URL, BuildConfig.API_BASE_URL)
        assertInstanceOf(BuildingsApi::class.java, api)
        val parsed = json.decodeFromString<BuildingListDto>("""{"source":"x","extra":1,"buildings":[]}""")
        assertEquals("x", parsed.source)
        assertEquals(emptyList<BuildingDto>(), parsed.buildings)
    }

    private companion object {
        const val CALL_MS = 3_000
        const val BASE_URL = "http://10.0.2.2:8080/"
    }
}
