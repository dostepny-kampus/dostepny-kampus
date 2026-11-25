/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Adres serwera z konfiguracji budowania: debug wskazuje komputer gospodarza emulatora, release tylko `https://`.
 *
 * Wymagania: NFR-36
 */
class ServerAddressTest {
    @Test
    fun addressFitsBuildType() {
        val url = BuildConfig.API_BASE_URL
        if (BuildConfig.BUILD_TYPE == RELEASE) {
            assertTrue(url.startsWith(HTTPS), "adres release $url bez HTTPS")
        } else {
            assertEquals(EMULATOR_HOST, url, "adres debug z emulatora")
        }
    }

    @Test
    fun addressEndsWithSlash() {
        assertTrue(BuildConfig.API_BASE_URL.endsWith("/"), "adres serwera bez ukośnika na końcu")
    }

    private companion object {
        const val RELEASE = "release"
        const val HTTPS = "https://"
        const val EMULATOR_HOST = "http://10.0.2.2:8080/"
    }
}
