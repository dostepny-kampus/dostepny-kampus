/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.network.building.BuildingsApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.util.concurrent.TimeUnit

/**
 * Wynik [call] na [BuildingsApi] połączonym z MockWebServer, który raz odpowiada [response], oraz ścieżka
 * z zapytaniem, o którą klient poprosił; `null`, gdy prośba nie doszła w sekundę.
 */
internal fun <T> served(response: MockResponse, call: suspend BuildingsApi.() -> T): Pair<T, String?> =
    MockWebServer().use { server ->
        server.enqueue(response)
        server.start()
        val api = BuildingsApi(http = OkHttpClient(), json = Json, baseUrl = server.url("/").toString())
        runBlocking { api.call() } to server.takeRequest(timeout = 1, unit = TimeUnit.SECONDS)?.path
    }
