/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.model.inspection.InspectionMarker
import io.github.dostepnykampus.core.model.inspection.MarkerPlace
import io.github.dostepnykampus.core.network.di.NetworkModule
import io.github.dostepnykampus.core.network.sync.SyncPull
import io.github.dostepnykampus.core.network.sync.SyncPullApi
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.Instant

/**
 * Pobranie zmian z serwera (`GET /sync/pull`): znaczniki pięter obejrzanych.
 *
 * Wymagania: US15.1, US15.2
 */
class SyncPullApiTest {
    @Test
    fun sendsSinceWhenGiven() {
        val sent = served(code = 200, body = EMPTY) { it.pull(since = "kursor 1") }.second
        assertEquals(listOf("GET", "/sync/pull?since=kursor%201"), listOf(sent.method, sent.path), "żądanie z since")
    }

    @Test
    fun omitsSinceWhenAbsent() {
        val sent = served(code = 200, body = EMPTY) { it.pull(since = null) }.second
        assertEquals(listOf("GET", "/sync/pull"), listOf(sent.method, sent.path), "żądanie bez since")
    }

    @Test
    fun readsCursorAndMarkers() {
        val result = served(code = 200, body = WITH_MARKERS) { it.pull(since = null) }.first
        val floor =
            InspectionMarker(
                id = FLOOR_MARKER,
                buildingSymbol = "A-1",
                place = MarkerPlace.Floor("A-1-2"),
                createdAt = Instant.parse("2026-03-01T10:15:00Z"),
                isRevoked = false,
                isMine = true,
            )
        val whole =
            InspectionMarker(
                id = WHOLE_MARKER,
                buildingSymbol = "B-4",
                place = MarkerPlace.WholeBuilding,
                createdAt = Instant.parse("2026-03-01T09:00:00Z"),
                isRevoked = true,
                isMine = false,
            )
        assertEquals(ApiResult.Success(SyncPull("c-17", listOf(floor, whole))), result, "kursor i znaczniki pięter")
    }

    @Test
    fun readsEmptyMarkers() {
        val result = served(code = 200, body = EMPTY) { it.pull(since = "c-1") }.first
        assertEquals(ApiResult.Success(SyncPull("c-2", emptyList())), result, "pusta lista znaczników")
    }

    @ParameterizedTest
    @ValueSource(ints = [400, 401, 500])
    fun refusalIsServerError(code: Int) {
        val result = served(code = code, body = """{"type":"about:blank"}""") { it.pull(since = null) }.first
        assertEquals(ApiResult.ServerError(code), result, "kod $code przy pobraniu")
    }

    @Test
    fun treatsBadBodyAsServerError() {
        val result = served(code = 200, body = "to nie JSON") { it.pull(since = null) }.first
        val error = assertInstanceOf(ApiResult.ServerError::class.java, result, "zła treść jako błąd serwera")
        assertEquals(200, error.code, "kod odpowiedzi ze złą treścią")
        assertInstanceOf(IllegalArgumentException::class.java, error.cause, "przyczyna złej treści")
    }

    @Test
    fun treatsBadInstantAsServerError() {
        val body = WITH_MARKERS.replace(oldValue = "2026-03-01T10:15:00Z", newValue = "wczoraj")
        val result = served(code = 200, body = body) { it.pull(since = null) }.first
        val error = assertInstanceOf(ApiResult.ServerError::class.java, result, "zła chwila znacznika")
        assertInstanceOf(IllegalArgumentException::class.java, error.cause, "przyczyna złej chwili")
    }

    private fun <T> served(code: Int, body: String, call: suspend (SyncPullApi) -> T): Pair<T, RecordedRequest> {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(code).setBody(body))
            server.start()
            val api = SyncPullApi(OkHttpClient(), NetworkModule.json(), server.url("/").toString())
            return runBlocking { call(api) } to server.takeRequest()
        }
    }

    private companion object {
        const val FLOOR_MARKER = "3c2b1a09-8f7e-4d6c-b5a4-918273645546"
        const val WHOLE_MARKER = "5d4c3b2a-1f0e-4a9b-8c7d-6e5f4a3b2c1d"
        const val EMPTY = """{"cursor":"c-2","submissions":[],"entrances":[],"inspectionMarkers":[]}"""
        const val WITH_MARKERS =
            """{"cursor":"c-17","submissions":[],"entrances":[],"inspectionMarkers":[
                {"id":"$FLOOR_MARKER","buildingSymbol":"A-1","floorId":"A-1-2",
                 "createdAt":"2026-03-01T10:15:00Z","revoked":false,"mine":true},
                {"id":"$WHOLE_MARKER","buildingSymbol":"B-4",
                 "createdAt":"2026-03-01T09:00:00Z","revoked":true,"mine":false}]}"""
    }
}
