/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.account

import io.github.dostepnykampus.core.data.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Adres realmu logowania z konfiguracji budowania: debug wskazuje komputer gospodarza emulatora, release tylko
 * `https://`.
 *
 * Wymagania: NFR-36
 */
class LoginRealmAddressTest {
    @Test
    fun realmFitsBuildType() {
        val url = BuildConfig.LOGIN_REALM_URL
        if (BuildConfig.BUILD_TYPE == RELEASE) {
            assertTrue("realm release $url bez HTTPS", url.startsWith(HTTPS))
        } else {
            assertEquals("realm debug z emulatora", EMULATOR_REALM, url)
        }
    }

    @Test
    fun realmEndsWithSlash() {
        assertTrue("realm bez ukośnika na końcu", BuildConfig.LOGIN_REALM_URL.endsWith("/"))
    }

    private companion object {
        const val RELEASE = "release"
        const val HTTPS = "https://"
        const val EMULATOR_REALM = "http://10.0.2.2:8180/realms/dostepny-kampus/"
    }
}
