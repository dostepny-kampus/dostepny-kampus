/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.model.BarrierGroup
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionKind
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Token konta w żądaniach do serwera aplikacji; operacje publiczne i inne adresy bez tokenu. Wymagania: NFR-35 */
class AccessTokenInterceptorTest {
    private val server = MockWebServer()
    private val tokens = AccessTokens()
    private val json = NetworkModule.json()
    private lateinit var http: OkHttpClient
    private lateinit var base: String

    @BeforeEach
    fun start() {
        server.start()
        base = server.url("/api/").toString()
        http = OkHttpClient.Builder().addInterceptor(AccessTokenInterceptor(base, tokens)).build()
    }

    @AfterEach
    fun stop() {
        server.shutdown()
    }

    @Test
    fun create_carries_token() {
        tokens.signIn(TOKEN)
        server.enqueue(MockResponse().setResponseCode(CREATED))
        runBlocking { SubmissionsApi(http, json, base).create(ID, QUICK) }
        assertEquals("Bearer $TOKEN", server.takeRequest().getHeader(AUTHORIZATION))
    }

    @Test
    fun buildings_without_token() {
        tokens.signIn(TOKEN)
        server.enqueue(MockResponse().setBody("""{"source":"x","buildings":[]}"""))
        runBlocking { BuildingsApi(http, json, base).listBuildings() }
        val recorded = server.takeRequest()
        assertEquals("/api/buildings", recorded.path)
        assertNull(recorded.getHeader(AUTHORIZATION))
    }

    @Test
    fun other_address_without_token() {
        tokens.signIn(TOKEN)
        server.enqueue(MockResponse())
        http.newCall(Request.Builder().url(server.url("/kafelki/1/2/3.png")).build()).execute().close()
        assertNull(server.takeRequest().getHeader(AUTHORIZATION))
    }

    @Test
    fun signed_out_401_rejected() {
        tokens.signIn(TOKEN)
        tokens.signOut()
        server.enqueue(MockResponse().setResponseCode(UNAUTHORIZED))
        runBlocking {
            val rejection = async(start = CoroutineStart.UNDISPATCHED) { tokens.rejections.first() }
            val result = SubmissionsApi(http, json, base).create(ID, QUICK)
            assertEquals(Unit, rejection.await())
            assertInstanceOf(ApiResult.ServerError::class.java, result)
        }
        assertNull(server.takeRequest().getHeader(AUTHORIZATION))
    }

    @Test
    fun token_not_in_to_string() {
        tokens.signIn(TOKEN)
        assertEquals("AccessTokens(signedIn=true)", tokens.toString())
    }

    private companion object {
        const val TOKEN = "token-konta"
        const val ID = "6f1c2b9e-0000-4000-8000-000000000001"
        const val AUTHORIZATION = "Authorization"
        const val CREATED = 201
        const val UNAUTHORIZED = 401
        val QUICK =
            SubmissionContent(
                kind = SubmissionKind.QUICK,
                location = GeoPoint(longitude = 17.0, latitude = 51.0),
                photoIds = listOf("zdjecie-1"),
                barrierGroup = BarrierGroup.OTHER,
            )
    }
}
