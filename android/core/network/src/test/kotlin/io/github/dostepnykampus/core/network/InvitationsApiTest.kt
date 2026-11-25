/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.model.StaffRole
import io.github.dostepnykampus.core.network.di.NetworkModule
import io.github.dostepnykampus.core.network.invitation.InvitationsApi
import io.github.dostepnykampus.core.network.invitation.Invited
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Zaproszenie osoby z rolą (`POST /invitations`).
 *
 * Wymagania: US12.1, US12.2, BR-2
 */
class InvitationsApiTest {
    @Test
    fun postsEmailAndRole() {
        val (result, sent) = served(code = 201, body = answer(existed = false))
        assertEquals(ApiResult.Success(Invited(isExistingAccount = false)), result, "nowe konto")
        assertEquals(listOf("POST", "/invitations"), listOf(sent.method, sent.path), "metoda i ścieżka żądania")
        val expected = """{"email":"$EMAIL","role":"VERIFIER"}"""
        assertEquals(expected, sent.body.readUtf8(), "adres i rola w treści")
    }

    @Test
    fun readsExistingAccount() {
        val result = served(code = 201, body = answer(existed = true)).first
        assertEquals(ApiResult.Success(Invited(isExistingAccount = true)), result, "rola dodana do konta")
    }

    @Test
    fun badRequestIsServerError() {
        val result = served(code = 400, body = """{"type":"about:blank"}""").first
        assertEquals(ApiResult.ServerError(400), result, "zły adres")
    }

    private fun answer(existed: Boolean): String = """{"email":"$EMAIL","role":"VERIFIER","accountExisted":$existed}"""

    private fun served(code: Int, body: String): Pair<ApiResult<Invited>, RecordedRequest> {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(code).setBody(body))
            server.start()
            val api = InvitationsApi(OkHttpClient(), NetworkModule.json(), server.url("/").toString())
            return runBlocking { api.invite(EMAIL, StaffRole.VERIFIER) } to server.takeRequest()
        }
    }

    private companion object {
        const val EMAIL = "anna.nowak@pwr.edu.pl"
    }
}
