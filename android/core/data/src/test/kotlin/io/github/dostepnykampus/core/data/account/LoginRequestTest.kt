/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.account

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Żądanie logowania: Authorization Code z PKCE S256, klient publiczny i przekierowanie aplikacji.
 *
 * Wymagania: US14.2, US14.3, NFR-31
 */
@RunWith(RobolectricTestRunner::class)
class LoginRequestTest {
    @Test
    fun usesPkceForAppClient() {
        val request = loginRequest(SignInEntry.SIGN_IN, REALM)
        val actual =
            listOf(
                request.codeVerifierChallengeMethod,
                request.clientId,
                request.redirectUri.toString(),
                request.responseType,
                request.configuration.authorizationEndpoint.toString(),
                request.configuration.tokenEndpoint.toString(),
                request.prompt,
            )
        val expected =
            listOf(
                "S256",
                "dostepny-kampus-android",
                "io.github.dostepnykampus:/oauth2redirect",
                "code",
                "http://auth.test/realms/kampus/protocol/openid-connect/auth",
                "http://auth.test/realms/kampus/protocol/openid-connect/token",
                null,
            )
        assertEquals("żądanie logowania", expected, actual)
    }

    @Test
    fun opensRegistration() {
        assertEquals("rejestracja", "create", loginRequest(SignInEntry.REGISTER, REALM).prompt)
    }

    private companion object {
        val REALM: Uri = Uri.parse("http://auth.test/realms/kampus/")
    }
}
