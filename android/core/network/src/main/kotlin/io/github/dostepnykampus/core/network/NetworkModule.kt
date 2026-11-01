/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Klient HTTP `core:network` w grafie Hilt aplikacji (decyzja 97).
 *
 * Jedna instancja klienta, parsera i [BuildingsApi] na proces.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val CONNECT_TIMEOUT_SECONDS = 10L
    private const val READ_TIMEOUT_SECONDS = 30L
    private const val WRITE_TIMEOUT_SECONDS = 30L

    /** Klient HTTP: 10 s na połączenie, 30 s na odczyt i na zapis. */
    @Provides
    @Singleton
    fun okHttp(): OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

    /** Parser kontraktu. Nieznane pole odpowiedzi jest pomijane, bo serwer może dodać pola później. */
    @Provides
    @Singleton
    fun json(): Json = Json { ignoreUnknownKeys = true }

    /**
     * Lista budynków, karta i podpowiedź punktu.
     *
     * Adres to [BuildConfig.API_BASE_URL]. Z emulatora `http://10.0.2.2:8080/` wskazuje serwer na komputerze
     * gospodarza.
     */
    @Provides
    @Singleton
    fun buildingsApi(http: OkHttpClient, json: Json): BuildingsApi =
        BuildingsApi(http = http, json = json, baseUrl = BuildConfig.API_BASE_URL)
}
